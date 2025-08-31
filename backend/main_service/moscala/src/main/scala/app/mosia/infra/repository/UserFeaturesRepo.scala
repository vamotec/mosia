package app.mosia.infra.repository

import app.mosia.domain.model.*
import app.mosia.infra.features.FeaturesType
import app.mosia.infra.features.UserFeatures.*
import app.mosia.infra.repository.registry.FeaturesRegistry.ConfigOf
import zio.*
import zio.json.JsonEncoder

import java.util.UUID
import javax.sql.DataSource

trait UserFeaturesRepo {
  def get[T <: FeaturesName](userId: UUID, name: T)(using
    FeatureConfigProvider[ConfigOf[T]],
    JsonEncoder[ConfigOf[T]]
  ): Task[Option[Features]]
  def getQuota[T <: FeaturesName](userId: UUID)(using
    FeatureConfigProvider[ConfigOf[T]],
    JsonEncoder[ConfigOf[T]]
  ): Task[Option[Features]]
  def has(userId: UUID, name: FeaturesName): Task[Boolean]
  def list(userId: UUID, `type`: Option[FeaturesType]): Task[List[Option[FeaturesName]]]
  def add[T <: FeaturesName](
    userId: UUID,
    name: T,
    reason: String
  )(using FeatureConfigProvider[ConfigOf[T]]): Task[UserFeatures]
  def remove(userId: UUID, name: FeaturesName): Task[Unit]
  def switchQuota[T <: FeaturesName](
    userId: UUID,
    to: T,
    reason: String
  )(using FeatureConfigProvider[ConfigOf[T]]): RIO[DataSource, UserFeatures]
}
