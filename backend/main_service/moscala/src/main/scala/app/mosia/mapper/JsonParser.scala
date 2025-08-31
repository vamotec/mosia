package app.mosia.mapper

import app.mosia.core.types.JSONValue
import app.mosia.core.types.JSONValue.fromJson
import zio.json.ast.Json
import zio.json.{ EncoderOps, JsonEncoder }
import zio.{ Task, ZIO }

object JsonParser:
  extension [C](config: C)(using JsonEncoder[C])
    def toJsonMap: Map[String, JSONValue] =
      new EncoderOps(config).toJsonAST match
        case Right(ast) =>
          fromJson(ast) match
            case Right(JSONValue.JSONObject(fields)) => fields
            case Right(_)                            => Map.empty
            case Left(_)                             => Map.empty
        case Left(_)    => Map.empty

  extension [T](config: T)(using encoder: JsonEncoder[T])
    def toJsonValue: Task[JSONValue] =
      ZIO
        .fromEither(encoder.toJsonAST(config).left.map(err => new RuntimeException(s"Invalid JSON: $err")))
        .flatMap(ast =>
          ZIO.fromEither(
            fromJson(ast).left
              .map(err => new RuntimeException(s"Failed to convert JSON AST: ${err.msgs.mkString("; ")}"))
          )
        )
