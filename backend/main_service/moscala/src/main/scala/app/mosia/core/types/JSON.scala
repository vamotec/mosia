package app.mosia.core.types

import caliban.CalibanError.ExecutionError
import caliban.ResponseValue
import caliban.Value.*
import caliban.schema.*
import zio.json.*

case class JSON(value: JSONValue)
object JSON:
  // 输出类型（Schema）
  given Schema[Any, JSON] = Schema.scalarSchema[JSON](
    name = "JSON",
    description = Some("Input as JSON string, output as structured JSON"),
    specifiedBy = None,
    directives = None,
    makeResponse = j => toResponseValue(j.value)
  )

  // 输入类型（ArgBuilder）
  given ArgBuilder[JSON] =
    ArgBuilder.string.flatMap { raw =>
      JsonDecoder[JSONValue].decodeJson(raw) match
        case Right(jv) => Right(JSON(jv))
        case Left(err) => Left(ExecutionError(s"Invalid JSON input: $err"))
    }

  private def toResponseValue(value: Any): ResponseValue = value match
    case null             => NullValue
    case s: String        => StringValue(s)
    case b: Boolean       => BooleanValue(b)
    case i: Int           => IntValue(i)
    case l: Long          => IntValue(BigInt(l))
    case d: Double        => FloatValue(BigDecimal(d))
    case f: Float         => FloatValue(BigDecimal(f.toDouble))
    case bd: BigDecimal   => FloatValue(bd)
    case bi: BigInt       => IntValue(bi)
    case m: Map[_, _]     =>
      ResponseValue.ObjectValue(
        m.toList.collect { case (k: String, v) =>
          k -> toResponseValue(v)
        }
      )
    case seq: Iterable[_] => ResponseValue.ListValue(seq.map(toResponseValue).toList)
    case arr: Array[_]    => ResponseValue.ListValue(arr.map(toResponseValue).toList)
    case other            => StringValue(other.toString)
