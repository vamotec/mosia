package app.mosia.core.errors

import sttp.tapir.Schema
import zio.json.{ DeriveJsonCodec, JsonCodec }

case class ErrorResponse(name: String, message: String) extends Throwable
object ErrorResponse:
  given JsonCodec[ErrorResponse] = DeriveJsonCodec.gen[ErrorResponse]
  given Schema[ErrorResponse]    = Schema.derived
