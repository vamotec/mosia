package app.mosia.infra.token

import app.mosia.core.configs.AppConfig
import app.mosia.infra.cache.{ CacheProvider, CacheProviderImpl, CacheSetOptions }
import zio.*
import zio.redis.Redis

import java.security.SecureRandom

case class TokenServiceImpl(
  cache: CacheProvider,
  configRef: Ref[AppConfig]
) extends TokenService:

  override def generatePasswordResetToken(email: String): Task[String] =
    for
      token <- ZIO.succeed(generateSecureToken())
      key    = s"password_reset:$token"
      _     <- cache.cache.set(key, email, CacheSetOptions(ttl = Some(24.hours))) // 24小时过期
    yield token

  override def generateSetPasswordToken(email: String, userId: String): Task[String] =
    for
      token <- ZIO.succeed(generateSecureToken())
      key    = s"set_password:$token"
      value  = s"$userId:$email"
      _     <- cache.cache.set(key, value, CacheSetOptions(ttl = Some(72.hours))) // 72小时过期
    yield token

  override def generateEmailVerificationToken(email: String, userId: String): Task[String] =
    for
      token <- ZIO.succeed(generateSecureToken())
      key    = s"email_verify:$token"
      value  = s"$userId:$email"
      _     <- cache.cache.set(key, value, CacheSetOptions(ttl = Some(24.hours))) // 24小时过期
    yield token

  override def validatePasswordResetToken(token: String): Task[Option[String]] =
    cache.cache.get(s"password_reset:$token")

  override def validateSetPasswordToken(token: String): Task[Option[(String, String)]] =
    cache.cache
      .get[String](s"set_password:$token")
      .map(_.map { value =>
        val parts = value.split(":", 2)
        (parts(0), parts(1)) // (userId, email)
      })

  override def validateEmailVerificationToken(token: String): Task[Option[(String, String)]] =
    cache.cache
      .get[String](s"email_verify:$token")
      .map(_.map { value =>
        val parts = value.split(":", 2)
        (parts(0), parts(1)) // (userId, email)
      })

  private def generateSecureToken(): String =
    // 生成安全的随机token
    val random = SecureRandom()
    val chars  = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    (1 to 32).map(_ => chars(random.nextInt(chars.length))).mkString

object TokenServiceImpl:
  def make: ZIO[Ref[AppConfig] & CacheProvider, Nothing, TokenServiceImpl] =
    for
      cache     <- ZIO.service[CacheProvider]
      configRef <- ZIO.service[Ref[AppConfig]]
    yield new TokenServiceImpl(cache, configRef)

  val live: ZLayer[Ref[AppConfig], Throwable, TokenService] = CacheProviderImpl.cacheCombine >>> ZLayer.fromZIO(make)
