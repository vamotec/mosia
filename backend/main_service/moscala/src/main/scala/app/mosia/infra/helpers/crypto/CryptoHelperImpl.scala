package app.mosia.infra.helpers.crypto

import app.mosia.core.configs.AppConfig
import app.mosia.core.errors.UserFriendlyError.*
import app.mosia.infra.eventbus.EventBus
import app.mosia.infra.events.ConfigEvent
import app.mosia.infra.helpers.crypto.CryptoUtils.*
import zio.{ Ref, Task, UIO, ZIO, ZLayer }

import java.security.{ MessageDigest, SecureRandom }
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.{ GCMParameterSpec, SecretKeySpec }

private case class CryptoHelperImpl(
  configRef: Ref[AppConfig],
  keyPairRef: Ref[Option[KeyPair]],
  eventBus: EventBus,
  random: SecureRandom
) extends CryptoHelper {
  override def init(): Task[Unit] =
    for {
      _      <- initProvider
      config <- configRef.get

//      privateKey <- ZIO
//                      .fromOption(config.crypto.privateKey)
//                      .orElse(CryptoUtils.generatePrivateKey())
//                      .tapError(e => ZIO.logError(s"Failed to get or generate private key: ${e.getMessage}"))
      privateKey <- CryptoUtils.generatePrivateKey()

      _ <- ZIO.logInfo(s"privateKey: $privateKey")
      // 从私钥生成公钥
      publicKey <- CryptoUtils
                     .generatePublicKey(privateKey)
                     .tapError(e => ZIO.logError(s"Failed to generate public key: ${e.getMessage}"))


      // 生成 SHA256 密钥
      sha256PublicKey  <- sha256(publicKey)
      sha256PrivateKey <- sha256(privateKey)

      // 创建密钥对
      keyPair = KeyPair(
                  publicKey = publicKey.getBytes("UTF-8"),
                  privateKey = privateKey.getBytes("UTF-8"),
                  sha256 = Sha256Keys(
                    publicKey = sha256PublicKey,
                    privateKey = sha256PrivateKey
                  )
                )

      // 更新密钥对引用
      _ <- keyPairRef.set(Some(keyPair))
      _ <- ZIO.logInfo("CryptoHelper initialized successfully")
    } yield ()

  override def sign(data: String): Task[String] =
    for {
      keyPair   <- keyPairRef.get.someOrFail(new RuntimeException("KeyPair not initialized"))
      signature <- ZIO.attempt {
                     val sign           = java.security.Signature.getInstance("SHA256withRSA")
                     sign.initSign(
                       java.security.KeyFactory
                         .getInstance("RSA")
                         .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(keyPair.privateKey))
                     )
                     sign.update(data.getBytes("UTF-8"))
                     val signatureBytes = sign.sign()
                     s"$data,${Base64.getEncoder.encodeToString(signatureBytes)}"
                   }
    } yield signature

  override def verify(signatureWithData: String): Task[Boolean] =
    for {
      keyPair <- keyPairRef.get.someOrFail(new RuntimeException("KeyPair not initialized"))
      result  <- ZIO.attempt {
                   val parts = signatureWithData.split(",")
                   if (parts.length != 2) false
                   else {
                     val data      = parts(0)
                     val signature = Base64.getDecoder.decode(parts(1))
                     val verify    = java.security.Signature.getInstance("SHA256withRSA")
                     verify.initVerify(
                       java.security.KeyFactory
                         .getInstance("RSA")
                         .generatePublic(new java.security.spec.X509EncodedKeySpec(keyPair.publicKey))
                     )
                     verify.update(data.getBytes("UTF-8"))
                     verify.verify(signature)
                   }
                 }
    } yield result

  override def encrypt(data: String): Task[String] =
    for {
      keyPair <- keyPairRef.get.someOrFail(new RuntimeException("KeyPair not initialized"))
      iv      <- randomBytes(NONCE_LENGTH) // 先获取 iv
      result  <- ZIO.attempt {
                   val cipher  = Cipher.getInstance("AES/GCM/NoPadding")
                   val keySpec = new SecretKeySpec(keyPair.sha256.privateKey, "AES")
                   val gcmSpec = new GCMParameterSpec(128, iv)
                   cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

                   val encrypted = cipher.doFinal(data.getBytes("UTF-8"))
                   val actualIv  = cipher.getIV

                   val result = Array.concat(actualIv, encrypted)
                   Base64.getEncoder.encodeToString(result)
                 }
    } yield result

  override def decrypt(encrypted: String): Task[String] =
    for {
      keyPair <- keyPairRef.get.someOrFail(new RuntimeException("KeyPair not initialized"))
      result  <- ZIO.attempt {
                   val buf           = Base64.getDecoder.decode(encrypted)
                   val iv            = buf.slice(0, NONCE_LENGTH)
                   val encryptedData = buf.slice(NONCE_LENGTH, buf.length)

                   val cipher  = Cipher.getInstance("AES/GCM/NoPadding")
                   val keySpec = new SecretKeySpec(keyPair.sha256.privateKey, "AES")
                   val gcmSpec = new GCMParameterSpec(128, iv)
                   cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

                   new String(cipher.doFinal(encryptedData), "UTF-8")
                 }
    } yield result

  override def compare(lhs: String, rhs: String): Task[Boolean] =
    ZIO.attempt {
      if (lhs.length != rhs.length) false
      else {
        var result = 0
        for (i <- lhs.indices)
          result |= lhs(i) ^ rhs(i)
        result == 0
      }
    }

  override def randomBytes(length: Int = NONCE_LENGTH): Task[Array[Byte]] =
    ZIO.attempt {
      val bytes = new Array[Byte](length)
      random.nextBytes(bytes)
      bytes
    }

  override def sha256(data: String): Task[Array[Byte]] =
    ZIO.attempt {
      MessageDigest.getInstance("SHA-256").digest(data.getBytes("UTF-8"))
    }

  override def registerListeners(): UIO[Unit] = for {
    _ <- eventBus.onEvent[ConfigEvent.Changed] {
           case ConfigEvent.Changed(updates) =>
             for {
               _ <- configRef.set(updates)
               _ <- init()
               _ <- ZIO.logInfo("CryptoService reinitialized after config change")
             } yield ()
           case null                         => ZIO.unit
         }
  } yield ()

  override def isValidEmail(email: String): Task[Boolean] =
    ZIO.attempt(email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))

  override def isValidPassword(password: String): Task[Boolean] = ZIO.attempt:
    val lengthOk   = password.length >= 8 && password.length <= 16
    val hasLetter  = password.exists(_.isLetter)
    val hasDigit   = password.exists(_.isDigit)
    val hasSpecial = password.exists(ch => !ch.isLetterOrDigit)
    lengthOk && hasLetter && hasDigit && hasSpecial
}

object CryptoHelperImpl:
  private def make(configRef: Ref[AppConfig], eventBus: EventBus): ZIO[Any, Throwable, CryptoHelper] =
    for
      keyPairRef <- Ref.make[Option[KeyPair]](None)
      random      = new SecureRandom()
      service     = new CryptoHelperImpl(configRef, keyPairRef, eventBus, random)
      _          <- service.init()
      _          <- service.registerListeners()
    yield service
  // 创建服务层
  val live: ZLayer[EventBus & Ref[AppConfig], Throwable, CryptoHelper]                               = ZLayer:
    for
      configRef <- ZIO.service[Ref[AppConfig]]
      eventBus  <- ZIO.service[EventBus]
      service   <- make(configRef, eventBus)
    yield service
