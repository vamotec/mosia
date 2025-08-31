package app.mosia.application.dto

import sttp.tapir.Schema
import zio.json.{ DeriveJsonCodec, JsonCodec }

case class JwtDto(value: String)
object JwtDto:
  given JsonCodec[JwtDto] = DeriveJsonCodec.gen[JwtDto]
  given Schema[JwtDto]    = Schema.derived
