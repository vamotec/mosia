package app.mosia.infra.dao

import app.mosia.core.types.JSONValue
import app.mosia.models.DbConfigs
import zio.RIO
import zio.json.ast.Json

import java.util.UUID
import javax.sql.DataSource

trait ConfigsDao:
  def findAll(): RIO[DataSource, List[DbConfigs]]
  def upsert(
    key: String,
    value: JSONValue,
    userId: UUID
  ): RIO[DataSource, DbConfigs]
