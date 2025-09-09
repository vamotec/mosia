package app.mosia.application.dto

import sttp.tapir.Schema
import zio.json.JsonCodec

case class MessageResponseDto(msg: String) derives JsonCodec, Schema
