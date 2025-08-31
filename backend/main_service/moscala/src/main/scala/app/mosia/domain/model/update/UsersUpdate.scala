package app.mosia.domain.model.update

import app.mosia.domain.model.Password

case class UsersUpdate(
  password: Option[Password] = None,
  name: Option[String] = None,
  avatarUrl: Option[String] = None,
  emailVerified: Option[Boolean] = None,
  registered: Option[Boolean] = None,
  disabled: Option[Boolean] = None
):
  avatarUrl.foreach { url =>
    require(url.startsWith("http://") || url.startsWith("https://"), s"Invalid avatar URL: $url")
  }
