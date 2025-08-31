package app.mosia.application.dto

import sttp.tapir.Schema
import zio.json.JsonCodec

case class EmailCheckResponseDto(registered: Boolean, hasPassword: Boolean) derives JsonCodec, Schema
