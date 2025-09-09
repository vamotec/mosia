package app.mosia.core.types

import caliban.CalibanError.ExecutionError
import caliban.InputValue
import caliban.schema.{ ArgBuilder, Schema }
import app.mosia.core.errors.ErrorMessage
import zio.json.*
import zio.json.ast.Json
import zio.json.internal.Write

sealed trait JSONValue derives Schema.SemiAuto, ArgBuilder
object JSONValue:
  case class JSONString(value: String)                  extends JSONValue derives Schema.SemiAuto, ArgBuilder
  case class JSONNumber(value: BigDecimal)              extends JSONValue derives Schema.SemiAuto, ArgBuilder
  case class JSONBoolean(value: Boolean)                extends JSONValue derives Schema.SemiAuto, ArgBuilder
  case class JSONObject(fields: Map[String, JSONValue]) extends JSONValue derives Schema.SemiAuto, ArgBuilder
  case class JSONArray(values: List[JSONValue])         extends JSONValue derives Schema.SemiAuto, ArgBuilder
  case class JSONNull()                                 extends JSONValue derives Schema.SemiAuto, ArgBuilder

  given ArgBuilder[Map[String, JSONValue]] =
    case InputValue.ObjectValue(fields) =>
      fields.toList.map { case (k, v) =>
        summon[ArgBuilder[JSONValue]].build(v).map(k -> _)
      }
        .foldRight[Either[ExecutionError, Map[String, JSONValue]]](Right(Map.empty)) {
          case (Right((k, v)), Right(acc)) => Right(acc + (k -> v))
          case (Left(e), _)                => Left(e)
          case (_, Left(e))                => Left(e)
        }
    case other                          =>
      Left(ExecutionError(s"Expected InputValue.ObjectValue for Map[String, JSONValue], but got $other"))

  /**
   * Json Serialization
   */
  given JsonEncoder[JSONValue] with
    override def unsafeEncode(a: JSONValue, indent: Option[Int], out: Write): Unit = a match
      case JSONString(v)  => JsonEncoder.string.unsafeEncode(v, indent, out)
      case JSONNumber(v)  => JsonEncoder.bigDecimal.unsafeEncode(v.bigDecimal, indent, out)
      case JSONBoolean(v) => JsonEncoder.boolean.unsafeEncode(v, indent, out)
      case JSONNull()     => out.write("null")
      case JSONArray(vs)  => JsonEncoder.list(using summon[JsonEncoder[JSONValue]]).unsafeEncode(vs, indent, out)
      case JSONObject(fs) =>
        JsonEncoder.map(JsonFieldEncoder.string, summon[JsonEncoder[JSONValue]]).unsafeEncode(fs, indent, out)

  given JsonDecoder[JSONValue] = JsonDecoder[Json].mapOrFail: json =>
    fromJson(json) match
      case Right(value) => Right(value)
      case Left(err)    => Left(err.msgs.mkString("; "))

  def fromJson(json: Json): Either[ErrorMessage, JSONValue] = json match
    case Json.Str(v)      => Right(JSONString(v))
    case Json.Num(v)      => Right(JSONNumber(v))
    case Json.Bool(v)     => Right(JSONBoolean(v))
    case Json.Null        => Right(JSONNull())
    case Json.Arr(items)  =>
      items
        .map(fromJson)
        .foldRight[Either[ErrorMessage, List[JSONValue]]](Right(Nil)) {
          case (Right(v), Right(acc)) => Right(v :: acc)
          case (Left(e), _)           => Left(e)
          case (_, Left(e))           => Left(e)
        }
        .map(JSONArray(_))
    case Json.Obj(fields) =>
      fields.map { case (k, v) => fromJson(v).map(k -> _) }
        .foldRight[Either[ErrorMessage, Map[String, JSONValue]]](Right(Map.empty)) {
          case (Right((k, v)), Right(acc)) => Right(acc + (k -> v))
          case (Left(e), _)                => Left(e)
          case (_, Left(e))                => Left(e)
        }
        .map(JSONObject(_))
