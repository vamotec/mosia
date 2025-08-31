package app.mosia.infra.repository.impl

import app.mosia.infra.dao.DaoModule
import app.mosia.domain.model.*
import app.mosia.domain.model.input.UserFeaturesInput
import app.mosia.infra.dao.impl.DaoModuleImpl
import app.mosia.infra.dao.impl.QuillContext.transaction
import app.mosia.infra.features.FeaturesType
import app.mosia.infra.features.UserFeatures.*
import app.mosia.infra.repository.registry.FeaturesRegistry
import app.mosia.infra.repository.registry.FeaturesRegistry.ConfigOf
import app.mosia.infra.repository.{ FeaturesRepo, UserFeaturesRepo }
import app.mosia.mapper.DomainMappers.*
import app.mosia.models.DbUserFeatures
import zio.*
import zio.json.JsonEncoder

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

private case class UserFeaturesRepoImpl(dao: DaoModule, featuresRepo: FeaturesRepo, ds: DataSource)
    extends UserFeaturesRepo:
  override def get[T <: FeaturesName](userId: UUID, name: T)(using
    FeatureConfigProvider[FeaturesRegistry.ConfigOf[T]],
    JsonEncoder[ConfigOf[T]]
  ): Task[Option[Features]] =
    for
      count  <- dao.userFeaturesDao.count(userId, name.value).provideEnvironment(ZEnvironment(ds))
      result <-
        if (count == 0) ZIO.none
        else featuresRepo.get[T](name).map(Some(_))
    yield result

  override def getQuota[T <: FeaturesName](userId: UUID)(using
    FeatureConfigProvider[FeaturesRegistry.ConfigOf[T]],
    JsonEncoder[ConfigOf[T]]
  ): Task[Option[Features]] =
    for {
      quotaOpt <- dao.userFeaturesDao
                    .findType(userId = userId, `type` = FeaturesType.Quota.toInt, activated = true)
                    .provideEnvironment(ZEnvironment(ds))
      result   <- quotaOpt match {
                    case Some(quota) =>
                      FeaturesName.fromString(quota.name) match {
                        case Some(name) => featuresRepo.get[T](name.asInstanceOf[T]).map(Some(_))
                        case None       => ZIO.none
                      }
                    case None        => ZIO.none
                  }
    } yield result

  override def has(userId: UUID, name: FeaturesName): Task[Boolean] =
    for count <- dao.userFeaturesDao.count(userId, name.value).provideEnvironment(ZEnvironment(ds))
    yield count > 0

  override def list(userId: UUID, `type`: Option[FeaturesType]): Task[List[Option[FeaturesName]]] =
    for
      dbNames <- `type` match
                   case Some(t) => dao.userFeaturesDao.listType(userId, t.toInt).provideEnvironment(ZEnvironment(ds))
                   case None    => dao.userFeaturesDao.list(userId).provideEnvironment(ZEnvironment(ds))
      names   <- toDomainList(dbNames)
      result   = names.map(f => FeaturesName.fromString(f.name))
    yield result

  override def add[T <: FeaturesName](
    userId: UUID,
    name: T,
    reason: String
  )(using FeatureConfigProvider[FeaturesRegistry.ConfigOf[T]]): Task[UserFeatures] =
    for
      feature    <- featuresRepo.get_unchecked(name)
      featureType = featuresRepo.getFeatureType[T]
      existing   <- dao.userFeaturesDao.findName(userId, name.value).provideEnvironment(ZEnvironment(ds))
      db         <- existing match
                      case Some(e) => ZIO.succeed(e)
                      case None    =>
                        for
                          userFeature <- dao.userFeaturesDao
                                           .create(
                                             UserFeaturesInput(
                                               userId = userId,
                                               featureId = feature.id,
                                               reason = reason,
                                               activated = true,
                                               expiredAt = None,
                                               name = name.value,
                                               `type` = featureType.toInt
                                             )
                                           )
                                           .provideEnvironment(ZEnvironment(ds))
                          _           <- ZIO.logInfo(s"Feature $name added to user $userId")
                        yield userFeature
      result     <- toDomain(db)
    yield result

  override def remove(userId: UUID, name: FeaturesName): Task[Unit] =
    for
      feature <- dao.userFeaturesDao.findName(userId, name.value).provideEnvironment(ZEnvironment(ds))
      _       <- feature match
                   case Some(f) =>
                     val input = UserFeaturesInput(
                       userId = userId,
                       featureId = f.featureId,
                       reason = "manually deactivated",
                       activated = false,
                       name = f.name,
                       expiredAt = Some(Instant.now()),
                       `type` = f.`type`
                     )
                     for
                       count <- dao.userFeaturesDao.update(input).provideEnvironment(ZEnvironment(ds))
                       _     <-
                         if (count > 0)
                           ZIO.logInfo(s"Feature $name deactivated for user $userId")
                         else
                           ZIO.logWarning(s"Attempted to deactivate $name for $userId, but nothing updated")
                     yield ()
                   case None    =>
                     ZIO.logWarning(s"Feature $name not found for user $userId, nothing to remove.")
    yield ()

  override def switchQuota[T <: FeaturesName](
    userId: UUID,
    to: T,
    reason: String
  )(using FeatureConfigProvider[FeaturesRegistry.ConfigOf[T]]): RIO[DataSource, UserFeatures] = transaction:
    for
      rawQuotas <- list(userId, Some(FeaturesType.Quota))
      quotas     = rawQuotas.flatten
      result    <- quotas match
                     case Nil                       => add(userId, to, reason)
                     case list if list.contains(to) =>
                       ZIO.fail(new IllegalStateException(s"Quota $to already active for user $userId"))
                     case list                      =>
                       for
                         _     <-
                           if (list.size > 1)
                             ZIO.logError(
                               s"User $userId has multiple quotas: ${list.mkString(", ")}, please check the database state."
                             )
                           else ZIO.unit
                         from  <- ZIO.succeed(list.last)
                         _     <- remove(userId, from)
                         added <- add(userId, to, reason)
                       yield added
    yield result

object UserFeaturesRepoImpl:
  def make: ZIO[DaoModule & FeaturesRepo & DataSource, Nothing, UserFeaturesRepo] =
    for
      features <- ZIO.service[FeaturesRepo]
      dao      <- ZIO.service[DaoModule]
      ds       <- ZIO.service[DataSource]
    yield new UserFeaturesRepoImpl(dao, features, ds)

  private val env: ZLayer[DataSource, Throwable, DaoModule & FeaturesRepo] =
    DaoModuleImpl.layer ++ FeaturesRepoImpl.live

  val live: ZLayer[DataSource, Throwable, UserFeaturesRepo] = env >>> ZLayer.fromZIO(make)
