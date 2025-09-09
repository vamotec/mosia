package app.mosia.infra.repository.impl

import app.mosia.core.configs.AppConfig
import app.mosia.infra.dao.DaoModule
import app.mosia.infra.eventbus.EventBus
import app.mosia.infra.repository.*
import zio.*

import javax.sql.DataSource

case class RepoModuleImpl(
  usersRepo: UsersRepo,
  sessionRepo: SessionRepo,
  workspaceUserRepo: WorkspaceUserRepo,
  configsRepo: ConfigsRepo,
  featuresRepo: FeaturesRepo,
  userFeaturesRepo: UserFeaturesRepo,
  verifyTokenRepo: VerifyTokenRepo
) extends RepoModule

object RepoModuleImpl:
  val layer: ZLayer[DataSource & Ref[AppConfig] & EventBus, Throwable, RepoModule] =
    (UsersRepoImpl.live ++
      SessionRepoImpl.live ++
      FeaturesRepoImpl.live ++
      UserFeaturesRepoImpl.live ++
      ConfigsRepoImpl.live ++
      VerifyTokenRepoImpl.live ++
      WorkspaceUserRepoImpl.live).map { env =>
      ZEnvironment(
        RepoModuleImpl(
          usersRepo = env.get[UsersRepo],
          sessionRepo = env.get[SessionRepo],
          workspaceUserRepo = env.get[WorkspaceUserRepo],
          configsRepo = env.get[ConfigsRepo],
          featuresRepo = env.get[FeaturesRepo],
          userFeaturesRepo = env.get[UserFeaturesRepo],
          verifyTokenRepo = env.get[VerifyTokenRepo]
        )
      )
    }
