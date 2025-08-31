package app.mosia.domain.model

import app.mosia.domain.model.AuthenticationType.*
import app.mosia.domain.model.Email.Email
import app.mosia.domain.model.Id.UserId
import app.mosia.domain.model.update.UsersUpdate
import org.mindrot.jbcrypt.BCrypt
import zio.json.*

import java.time.OffsetDateTime
import java.util.UUID

case class Users(
  id: UserId,
  email: Email,
  name: String,
  registered: Boolean = true,
  disabled: Boolean = false,
  passwordHash: Option[Password],
  avatarUrl: Option[String] = None,
  emailVerified: Option[OffsetDateTime] = None,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime
) derives JsonCodec:
  // 判断认证方式
  def authenticationType: AuthenticationType =
    if (passwordHash.isDefined) EmailPassword else OAuth

  // 验证密码（只对邮箱+密码用户有效）
  def validatePassword(plainPassword: String): Boolean =
    passwordHash.exists(_.verify(plainPassword))

  // 检查是否有密码
  def hasPassword: Boolean = passwordHash.isDefined

  // 检查是否可以密码登录
  def canLoginWithPassword: Boolean = hasPassword

  // 设置密码（OAuth用户也可以后续设置密码）
  def setPassword(plainPassword: String): Users =
    copy(
      passwordHash = Some(Password.fromPlainText(plainPassword)),
      updatedAt = OffsetDateTime.now()
    )

  // 移除密码（用户可以选择只用OAuth）
  def removePassword(): Users =
    copy(
      passwordHash = None,
      updatedAt = OffsetDateTime.now()
    )

  def isEmailVerified: Boolean = emailVerified.isDefined

  def updateProfile(newName: String, newAvatarUrl: Option[String]): Users =
    copy(
      name = newName,
      avatarUrl = newAvatarUrl,
      updatedAt = OffsetDateTime.now()
    )

  def verifyEmail: Users =
    copy(
      emailVerified = Some(OffsetDateTime.now()),
      updatedAt = OffsetDateTime.now()
    )

  def applyUpdate(update: UsersUpdate): Users =
    copy(
      passwordHash = update.password.orElse(passwordHash),
      name = update.name.getOrElse(name),
      avatarUrl = update.avatarUrl.orElse(avatarUrl),
      emailVerified = update.emailVerified
        .filter(identity) // 只有 true 才写入
        .map(_ => OffsetDateTime.now())
        .orElse(emailVerified),
      updatedAt = OffsetDateTime.now(),
      registered = update.registered.getOrElse(registered),
      disabled = update.disabled.getOrElse(disabled)
    )
