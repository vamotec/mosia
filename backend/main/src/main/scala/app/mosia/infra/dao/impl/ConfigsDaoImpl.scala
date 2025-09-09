package app.mosia.infra.dao.impl

import app.mosia.core.types.JSONValue
import app.mosia.infra.dao.{ ConfigsDao, WorkspacesDao }
import app.mosia.models.DbConfigs
import io.getquill.*
import io.getquill.context.ZioJdbc.*
import zio.json.ast.Json
import zio.{ RIO, URLayer, ZEnvironment, ZIO, ZLayer }

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

case class ConfigsDaoImpl(dataSource: DataSource) extends ConfigsDao:
  import QuillContext.{ *, given }

  override def findAll(): RIO[DataSource, List[DbConfigs]] =
    inline def queries = quote:
      DbConfigs.schema

    run(queries)

  override def upsert(
    key: String,
    value: JSONValue,
    userId: UUID
  ): RIO[DataSource, DbConfigs] =
    val now                   = Instant.now()
    val userOpt: Option[UUID] = Some(userId)
    inline def queries        = quote:
      DbConfigs.schema
        .insert(
          _.id            -> lift(key),
          _.value         -> lift(value),
          _.updatedAt     -> lift(now),
          _.lastUpdatedBy -> lift(userOpt)
        )
        .onConflictUpdate(_.id)(
          (t, e) => t.value -> e.value,
          (t, _) => t.updatedAt -> lift(now),
          (t, _) => t.lastUpdatedBy -> lift(userOpt)
        )
        .returning(r => r)

    run(queries)

object ConfigsDaoImpl:
  val live: URLayer[DataSource, ConfigsDao] = ZLayer.fromFunction(ConfigsDaoImpl.apply _)
