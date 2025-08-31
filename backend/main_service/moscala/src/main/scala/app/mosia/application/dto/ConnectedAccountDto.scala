package app.mosia.application.dto

case class ConnectedAccountDto(
  userId: String,
  provider: String,
  providerAccountId: String,
  scope: Option[String],
  accessToken: Option[String],
  refreshToken: Option[String],
  expiresAt: Option[java.time.Instant]
)
