package app.mosia.application.dto

import zio.json.*

import java.time.OffsetDateTime

case class LoginResponseDto(
  token: String,
  user: UserResponseDto,
  refreshToken: Option[String] = None,
  expiresAt: OffsetDateTime
) derives JsonCodec
