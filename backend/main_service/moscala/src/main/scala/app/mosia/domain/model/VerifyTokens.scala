package app.mosia.domain.model

import java.util.UUID

case class VerifyTokens(
  token: UUID,
  `type`: Int,
  credential: Option[String],
  expiresAt: java.time.Instant
)
