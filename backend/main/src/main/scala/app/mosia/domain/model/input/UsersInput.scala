package app.mosia.domain.model.input

import app.mosia.domain.model.Email.Email
import app.mosia.domain.model.*

import java.time.OffsetDateTime

case class UsersInput(
  email: String,
  name: String,
  password: Option[Password],
  avatarUrl: Option[String] = None,
  emailVerified: Option[OffsetDateTime] = None
):
  // 业务规则：avatarUrl 必须是有效的 URL（简单检查）
  avatarUrl.foreach { url =>
    require(url.startsWith("http://") || url.startsWith("https://"), s"Invalid avatar URL: $url")
  }

object UsersInput:
  // 创建邮箱+密码用户
  def withPassword(
    email: String,
    plainPassword: String,
    name: String,
    avatarUrl: Option[String] = None
  ): UsersInput =
    UsersInput(
      email = email,
      password = Some(Password.fromPlainText(plainPassword)),
      name = name,
      avatarUrl = avatarUrl
    )

  // 创建OAuth用户
  def oauth(
    email: String,
    name: String,
    avatarUrl: Option[String] = None,
    emailVerified: Boolean = true
  ): UsersInput =
    UsersInput(
      email = email,
      password = None,
      name = name,
      avatarUrl = avatarUrl
    )
