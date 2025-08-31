package app.mosia.domain.model.input

import app.mosia.infra.token.TokenType

import java.util.UUID

case class VerifyTokenInput(
  tokenType: TokenType,
  token: UUID,
  credential: Option[String],
  expiresAt: java.time.Instant
)
