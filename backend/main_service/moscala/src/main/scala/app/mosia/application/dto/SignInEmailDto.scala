package app.mosia.application.dto

case class SignInEmailDto(
  email: String,
  code: String,
  userName: String,
  ipAddress: Option[String] = None,
  userAgent: Option[String] = None,
  location: Option[String] = None
)
