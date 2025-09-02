package app.mosia.infra.features

import app.mosia.core.configs.AppConfig
import app.mosia.domain.model.Email.Email
import app.mosia.domain.model.UserFeatures
import app.mosia.infra.auth.Types.EarlyAccessType
import app.mosia.infra.eventbus.EventBus
import app.mosia.infra.features.UserFeatures.FeaturesName.*
import app.mosia.infra.repository.RepoModule
import app.mosia.infra.repository.impl.RepoModuleImpl
import zio.*

import java.util.UUID
import javax.sql.DataSource

case class FeatureServiceImpl(repo: RepoModule, configRef: Ref[AppConfig]) extends FeatureService:
  import FeatureServiceImpl.*

  override def isStaff(email: Email): Task[Boolean] = ZIO.succeed(STAFF.exists(email.value.endsWith))

  override def isAdmin(userId: UUID): Task[Boolean] = repo.userFeaturesRepo.has(userId, Administrator)

  override def addAdmin(userId: UUID): Task[UserFeatures] =
    repo.userFeaturesRepo.add[Administrator.type](userId, Administrator, "Admin user")

  override def addEarlyAccess(userId: UUID, `type`: EarlyAccessType): Task[UserFeatures] =
    `type` match
      case EarlyAccessType.App => repo.userFeaturesRepo.add[EarlyAccess.type](userId, EarlyAccess, "Early access user")
      case EarlyAccessType.AI  =>
        repo.userFeaturesRepo.add[AIEarlyAccess.type](userId, AIEarlyAccess, "AI Early access user")

  override def removeEarlyAccess(userId: UUID, `type`: EarlyAccessType): Task[Unit] =
    `type` match
      case EarlyAccessType.App => repo.userFeaturesRepo.remove(userId, EarlyAccess)
      case EarlyAccessType.AI  => repo.userFeaturesRepo.remove(userId, AIEarlyAccess)

  override def isEarlyAccessUser(userId: UUID, `type`: EarlyAccessType): Task[Boolean] =
    `type` match
      case EarlyAccessType.App => repo.userFeaturesRepo.has(userId, EarlyAccess)
      case EarlyAccessType.AI  => repo.userFeaturesRepo.has(userId, AIEarlyAccess)

  override def canEarlyAccess(email: Email, `type`: EarlyAccessType = EarlyAccessType.App): Task[Boolean] =
    for
      staff  <- isStaff(email)
      config <- configRef.get
      result <-
        if (config.flags.earlyAccessControl && !staff)
          for
            user  <- repo.usersRepo.getUserByEmail(email)
            value <- user match
                       case Some(u) => isEarlyAccessUser(u.id.value, `type`)
                       case None    => ZIO.succeed(false)
          yield value
        else
          ZIO.succeed(true)
    yield result

object FeatureServiceImpl:
  private val STAFF: Set[String] = Set("@vamotec.com", "@mosia.app")

  private def make: ZIO[Ref[AppConfig] & RepoModule, Nothing, FeatureService] =
    for
      repo      <- ZIO.service[RepoModule]
      configRef <- ZIO.service[Ref[AppConfig]]
    yield new FeatureServiceImpl(repo, configRef)

  val live: ZLayer[DataSource & Ref[AppConfig] & EventBus, Throwable, FeatureService] =
    RepoModuleImpl.layer >>> ZLayer.fromZIO(make)

end FeatureServiceImpl
