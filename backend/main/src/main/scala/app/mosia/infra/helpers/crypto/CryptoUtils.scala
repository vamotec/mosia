package app.mosia.infra.helpers.crypto

import app.mosia.core.errors.*
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.{ ECPrivateKeySpec, ECPublicKeySpec }
import zio.{ Task, ZIO }

import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.{ KeyFactory, KeyPairGenerator, Security }
import java.util.Base64

object CryptoUtils {
  val NONCE_LENGTH = 12

  case class Sha256Keys(publicKey: Array[Byte], privateKey: Array[Byte])

  case class KeyPair(publicKey: Array[Byte], privateKey: Array[Byte], sha256: Sha256Keys)

  // 添加 Bouncy Castle 作为安全提供者
  val initProvider: Task[Unit] = ZIO.attempt {
    Security.addProvider(new BouncyCastleProvider())
  }.tapError(_ => ZIO.logError("Failed to add BouncyCastle provider"))
    .tap(_ => ZIO.logInfo("Successfully added BouncyCastle provider"))
    .unit

  def generatePrivateKey(): Task[String] =
    for {
      _      <- initProvider
      result <- ZIO.attempt {
                  // 获取密钥对生成器
                  val keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC")
                  val curveParams      = ECNamedCurveTable.getParameterSpec("prime256v1")
                  keyPairGenerator.initialize(curveParams)

                  // 生成密钥对
                  val keyPair    = keyPairGenerator.generateKeyPair()
                  val privateKey = keyPair.getPrivate

                  // 将私钥转换为 PEM 格式
                  val encoded = privateKey.getEncoded
                  val base64  = Base64.getEncoder.encodeToString(encoded)

                  // 添加 PEM 头尾
                  s"""-----BEGIN RSA PRIVATE KEY-----
                     |$base64
                     |-----END RSA PRIVATE KEY-----""".stripMargin
                }.tapError(_ => ZIO.logError("Failed to generate private key"))
                  .tap(_ => ZIO.logDebug("Successfully generated private key"))
    } yield result

  def generatePublicKey(privateKeyPem: String): Task[String] =
    for {
      _      <- initProvider
      result <- ZIO.attempt {
                  // 验证输入格式
                  if (!validatePemFormat(privateKeyPem, "RSA PRIVATE KEY"))
                    throw InvalidKeyFormatError("Invalid private key format: missing PEM headers")

                  // 移除 PEM 头尾和换行符
                  val privateKeyBase64 = privateKeyPem
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "")

                  // 验证 Base64 格式
                  try
                    Base64.getDecoder.decode(privateKeyBase64)
                  catch {
                    case _: IllegalArgumentException =>
                      throw InvalidKeyFormatError("Invalid private key format: not valid Base64")
                  }

                  // 解码私钥
                  val privateKeyBytes = Base64.getDecoder.decode(privateKeyBase64)
                  val keySpec         = new PKCS8EncodedKeySpec(privateKeyBytes)

                  // 获取密钥工厂
                  val keyFactory = KeyFactory.getInstance("EC", "BC")
                  val privateKey = keyFactory.generatePrivate(keySpec)

                  val ecPrivateKey = privateKey.asInstanceOf[ECPrivateKey]
                  val d            = ecPrivateKey.getS // BigInteger 类型

                  // 从私钥生成公钥
                  val curveParams = ECNamedCurveTable.getParameterSpec("prime256v1")
                  val q           = curveParams.getG.multiply(d)

                  val publicKeySpec = new ECPublicKeySpec(q, curveParams)

                  val publicKey = keyFactory.generatePublic(publicKeySpec)

                  // 将公钥转换为 PEM 格式
                  val encoded = publicKey.getEncoded
                  val base64  = Base64.getEncoder.encodeToString(encoded)

                  // 添加 PEM 头尾
                  s"""-----BEGIN PUBLIC KEY-----
                     |$base64
                     |-----END PUBLIC KEY-----""".stripMargin
                }.tapError(_ => ZIO.logError("Failed to generate public key in def"))
                  .tap(_ => ZIO.logDebug("Successfully generated public key from private key"))
    } yield result

  // 辅助方法：验证密钥格式
  private def validatePemFormat(key: String, keyType: String): Boolean =
    key.contains(s"-----BEGIN $keyType-----") &&
      key.contains(s"-----END $keyType-----")
}
