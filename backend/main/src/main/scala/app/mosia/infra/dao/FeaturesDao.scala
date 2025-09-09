package app.mosia.infra.dao

import app.mosia.models.DbFeatures
import io.getquill.*
import zio.*

import javax.sql.DataSource

trait FeaturesDao {
  def findFirst(feature: String): RIO[DataSource, List[DbFeatures]]
}
