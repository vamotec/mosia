package app.mosia.domain.model.input

import java.util.UUID

case class ConnectedAccountInput(
  userId: UUID,
  provider: String,
  providerAccountId: String,
  scope: Option[String],
  accessToken: Option[String],
  refreshToken: Option[String],
  expiresAt: Option[java.time.Instant]
)
