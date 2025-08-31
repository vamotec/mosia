package app.mosia.application.dto

import zio.Duration

case class CookieWithMetaDto(
  name: String,
  value: String,
  maxAge: Option[Duration],
  secure: Boolean,
  httpOnly: Boolean,
  sameSite: Option[String],
  path: Option[String] = Some("/")
)
