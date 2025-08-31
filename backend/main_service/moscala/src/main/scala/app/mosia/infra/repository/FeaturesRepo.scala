package app.mosia.infra.repository

import app.mosia.core.types.JSONValue
import app.mosia.domain.model.Features
import app.mosia.infra.features.FeaturesType
import app.mosia.infra.features.UserFeatures.*
import app.mosia.infra.repository.registry.FeaturesRegistry.ConfigOf
import zio.Task
import zio.json.JsonEncoder
import zio.json.ast.Json
import zio.schema.Schema

import javax.sql.DataSource
import scala.reflect.ClassTag

trait FeaturesRepo:
  def get[T <: FeaturesName](name: T)(using
    FeatureConfigProvider[ConfigOf[T]],
    JsonEncoder[ConfigOf[T]]
  ): Task[Features]
  def get_unchecked(name: FeaturesName): Task[Features]
  def try_get_unchecked(name: FeaturesName): Task[List[Features]]
  def check[T <: FeaturesName](name: T, config: JSONValue)(using FeatureConfigProvider[ConfigOf[T]]): Task[ConfigOf[T]]
  def getFeatureType[T <: FeaturesName](using p: FeatureConfigProvider[ConfigOf[T]]): FeaturesType
