package app.mosia.infra.dao.impl

import app.mosia.infra.dao.FeaturesDao
import app.mosia.models.DbFeatures
import io.getquill.*
import zio.{ RIO, URLayer, ZIO, ZLayer }

import javax.sql.DataSource

case class FeaturesDaoImpl(dataSource: DataSource) extends FeaturesDao:
  import QuillContext.{ *, given }

  override def findFirst(feature: String): RIO[DataSource, List[DbFeatures]] =
    inline def queries = quote:
      DbFeatures.schema
        .filter(_.feature == lift(feature))
        .sortBy(_.id)(Ord.desc)
        .take(1)

    run(queries)

object FeaturesDaoImpl:
  val live: URLayer[DataSource, FeaturesDao] = ZLayer.fromFunction(FeaturesDaoImpl.apply _)
