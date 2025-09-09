package app.mosia.application.dto

import java.time.OffsetDateTime

case class UserSessionDto(
  id: String,
  userId: String,
  sessionId: String,
  createdAt: OffsetDateTime,
  lastAccessedAt: OffsetDateTime,
  expiresAt: OffsetDateTime,
  isActive: Boolean
)
