package app.mosia.application.dto

import app.mosia.domain.model.{ CurrentUser, Users }
import zio.json.*

import java.time.{ Instant, OffsetDateTime }
import java.util.UUID

// JWT 中存储的用户信息
case class JwtPayload(
  sub: String,               // subject - 用户ID
  email: String,             // 用户邮箱
  name: String,              // 用户姓名
  avatarUrl: Option[String], // 头像URL
  verified: Boolean,         // 邮箱是否已验证
  iat: Long,                 // issued at - 签发时间
  exp: Long,                 // expiration - 过期时间
  jti: Option[String] = None // JWT ID (可选，用于token撤销)
) derives JsonCodec

object JwtPayload:
  def fromUser(user: Users, expirationSeconds: Long = 86400): JwtPayload =
    val now = Instant.now().getEpochSecond
    JwtPayload(
      sub = user.id.value.toString,
      email = user.email.value,
      name = user.name,
      avatarUrl = user.avatarUrl,
      verified = user.isEmailVerified,
      iat = now,
      exp = now + expirationSeconds,
      jti = Some(UUID.randomUUID().toString)
    )
