package app.mosia.application.dto

import zio.json.JsonCodec

case class EmailRequestDto(
  email: String,
  userId: Option[String] = None,
  userName: Option[String] = None,
  redirectUrl: Option[String] = None
) derives JsonCodec
