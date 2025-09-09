package app.mosia.application.dto

import zio.json.*

case class UpdateUserDto(
  name: Option[String] = None,
  avatarUrl: Option[String] = None
) derives JsonCodec
