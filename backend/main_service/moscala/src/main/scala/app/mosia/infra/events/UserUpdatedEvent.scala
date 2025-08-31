package app.mosia.infra.events

import app.mosia.application.dto.UserResponseDto
import zio.json.JsonCodec

import java.time.OffsetDateTime

case class UserUpdatedEvent(
  id: String,
  updatedAt: OffsetDateTime = OffsetDateTime.now()
) derives JsonCodec
