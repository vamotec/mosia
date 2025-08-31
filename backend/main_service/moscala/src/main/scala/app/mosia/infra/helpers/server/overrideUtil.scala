package app.mosia.infra.helpers.server

import app.mosia.core.types.JSONValue
import app.mosia.core.types.JSONValue.fromJson
import zio.json.*
import zio.json.ast.Json

import java.util.UUID

object overrideUtil:
  def apply[T: JsonEncoder: JsonDecoder](base: T, updates: Map[String, JSONValue]): T =
    val baseJson  = base.toJsonAST.getOrElse(Json.Obj())
    val jsonValue = fromJson(baseJson) match
      case Left(err)    =>
        throw new RuntimeException(s"Failed to convert base Json to JSONValue: ${err.msgs.mkString("; ")}")
      case Right(value) => value
    val patched   = applyUpdates(jsonValue, updates)

    patched.toJson.fromJson[T] match
      case Left(err)  => throw new RuntimeException(s"Failed to decode updated config: $err")
      case Right(res) => res

  private def applyUpdates(json: JSONValue, updates: Map[String, JSONValue]): JSONValue =
    updates.foldLeft(json):
      case (currentJson, (path, value)) => setJsonPath(currentJson, path.split('.').toList, value)

  private def setJsonPath(json: JSONValue, path: List[String], value: JSONValue): JSONValue =
    def update(current: JSONValue, path: List[String], value: JSONValue): JSONValue = path match
      case Nil         => current
      case key :: Nil  =>
        val fields = current match
          case JSONValue.JSONObject(fs) => fs
          case _                        => Map.empty[String, JSONValue]
        JSONValue.JSONObject(fields.updated(key, value))
      case key :: tail =>
        val fields        = current match
          case JSONValue.JSONObject(fs) => fs
          case _                        => Map.empty[String, JSONValue]
        val nested        = fields.getOrElse(key, JSONValue.JSONObject(Map.empty))
        val updatedNested = update(nested, tail, value)
        JSONValue.JSONObject(fields.updated(key, updatedNested))

    update(json, path, value)
