package app.mosia.infra.repository.impl

import app.mosia.core.types.JSONValue
import app.mosia.domain.model.*
import app.mosia.infra.dao.DaoModule
import app.mosia.infra.dao.impl.DaoModuleImpl
import app.mosia.infra.dao.impl.QuillContext.dataSourceLayer
import app.mosia.infra.features.FeaturesType
import app.mosia.infra.features.UserFeatures.*
import app.mosia.infra.repository.FeaturesRepo
import app.mosia.infra.repository.registry.FeaturesRegistry.ConfigOf
import app.mosia.mapper.DomainMappers.*
import app.mosia.mapper.JsonParser.toJsonValue
import zio.*
import zio.json.ast.Json
import zio.json.{ EncoderOps, JsonEncoder }
import zio.schema.Schema

import javax.sql.DataSource
import scala.reflect.ClassTag

case class FeaturesRepoImpl(dao: DaoModule, ds: DataSource) extends FeaturesRepo:
  override def get[T <: FeaturesName](name: T)(using
    FeatureConfigProvider[ConfigOf[T]],
    JsonEncoder[ConfigOf[T]]
  ): Task[Features] =
    for
      feature <- get_unchecked(name)
      parsed  <- check(name, feature.configs)
      configs <- parsed.toJsonValue
    yield Features(
      id = feature.id,
      name = feature.name,
      configs = configs
    )

  override def get_unchecked(name: FeaturesName): Task[Features] =
    for
      uncheck <- try_get_unchecked(name)
      feature <- ZIO
                   .fromOption(uncheck.headOption)
                   .orElseFail(new RuntimeException(s"Feature ${name.value} not found"))
    yield feature

  override def try_get_unchecked(name: FeaturesName): Task[List[Features]] =
    for
      dbOpt  <- dao.featuresDao.findFirst(name.value).provideEnvironment(ZEnvironment(ds))
      result <- ZIO.foreach(dbOpt)(toDomain)
    yield result

  override def check[T <: FeaturesName](name: T, config: JSONValue)(using
    FeatureConfigProvider[ConfigOf[T]]
  ): Task[ConfigOf[T]] =
    val decoder = summon[FeatureConfigProvider[ConfigOf[T]]].decoder
    ZIO
      .fromEither(decoder.decodeJson(config.toJson))
      .mapError(err => new RuntimeException(s"Invalid config for feature [$name]: $err"))

  override def getFeatureType[T <: FeaturesName](using p: FeatureConfigProvider[ConfigOf[T]]): FeaturesType =
    p.featuresType

object FeaturesRepoImpl:
  def make: ZIO[DaoModule & DataSource, Nothing, FeaturesRepo] =
    for
      dao <- ZIO.service[DaoModule]
      ds  <- ZIO.service[DataSource]
    yield new FeaturesRepoImpl(dao, ds)

  val live: ZLayer[DataSource, Throwable, FeaturesRepo] = DaoModuleImpl.layer >>> ZLayer.fromZIO(make)
