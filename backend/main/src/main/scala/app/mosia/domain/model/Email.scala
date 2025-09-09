package app.mosia.domain.model

import app.mosia.models.DbFeatures
import zio.json.{ DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder }

object Email:
  opaque type Email = String

  def apply(value: String): Email =
    require(value.contains("@"), "Invalid email format")
    value

  extension (e: Email)
    def value: String    = e
    def username: String =
      e.split("@")
        .headOption
        .getOrElse(
          throw new IllegalArgumentException("Invalid email format, missing @")
        )

  // ✅ 显式指定使用 string 的 encoder/decoder，避免宏误判
  given JsonEncoder[Email] = JsonEncoder.string.contramap(_.value)

  given JsonDecoder[Email] = JsonDecoder.string.mapOrFail { str =>
    try Right(Email(str))
    catch {
      case e: IllegalArgumentException => Left(e.getMessage)
    }
  }
