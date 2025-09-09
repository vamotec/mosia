package app.mosia.core.errors

import sttp.tapir.Schema
import zio.json.{ DeriveJsonCodec, JsonCodec }

case class ErrorMessage(msgs: List[String]) extends Throwable
object ErrorMessage:
  given JsonCodec[ErrorMessage] = DeriveJsonCodec.gen[ErrorMessage]
  given Schema[ErrorMessage]    = Schema.derived
