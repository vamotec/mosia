package app.mosia.infra.dao.impl

import app.mosia.core.types.JSONValue
import app.mosia.core.types.JSONValue.JSONNull
import io.getquill.jdbczio.Quill
import io.getquill.{ SnakeCase, * }
import zio.ZLayer
import zio.json.*

import java.time.Instant
import javax.sql.DataSource

object QuillContext extends PostgresZioJdbcContext(SnakeCase):
  val dataSourceLayer: ZLayer[Any, Throwable, DataSource] =
    Quill.DataSource.fromPrefix("app.database")

  inline given MappedEncoding[String, JSONValue] =
    MappedEncoding(str => str.fromJson[JSONValue].getOrElse(JSONNull()))

  inline given MappedEncoding[JSONValue, String] =
    MappedEncoding(json => json.toJson)

  // Instant -> java.sql.Timestamp（写入）
  inline given MappedEncoding[Instant, java.sql.Timestamp] =
    MappedEncoding(instant => java.sql.Timestamp.from(instant))

  // java.sql.Timestamp -> Instant（读取）
  inline given MappedEncoding[java.sql.Timestamp, Instant] =
    MappedEncoding(timestamp => timestamp.toInstant)
