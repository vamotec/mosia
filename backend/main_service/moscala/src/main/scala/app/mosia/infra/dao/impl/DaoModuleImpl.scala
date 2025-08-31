package app.mosia.infra.dao.impl

import app.mosia.domain.model.VerifyTokens
import app.mosia.infra.dao.impl.QuillContext.dataSourceLayer
import app.mosia.infra.dao.*
import io.getquill.*
import zio.*

import javax.sql.DataSource

case class DaoModuleImpl(
  userConnectedAccountsDao: UserConnectedAccountsDao,
  usersDao: UsersDao,
  workspacesDao: WorkspacesDao,
  workspaceUserDao: WorkspaceUserDao,
  sessionsDao: SessionsDao,
  userSessionsDao: UserSessionsDao,
  featuresDao: FeaturesDao,
  userFeaturesDao: UserFeaturesDao,
  configsDao: ConfigsDao,
  verifyTokenDao: VerifyTokenDao
) extends DaoModule

object DaoModuleImpl:
  val layer: ZLayer[DataSource, Throwable, DaoModule] =
    (
      UserConnectedAccountsDaoImpl.live ++
        ConfigsDaoImpl.live ++
        UsersDaoImpl.live ++
        WorkspacesDaoImpl.live ++
        FeaturesDaoImpl.live ++
        UserFeaturesDaoImpl.live ++
        SessionsDaoImpl.live ++
        VerifyTokenDaoImpl.live ++
        UserSessionsDaoImpl.live ++
        WorkspaceUserDaoImpl.live
    ).map { env =>
      ZEnvironment(
        DaoModuleImpl(
          configsDao = env.get[ConfigsDao],
          userConnectedAccountsDao = env.get[UserConnectedAccountsDao],
          usersDao = env.get[UsersDao],
          featuresDao = env.get[FeaturesDao],
          userFeaturesDao = env.get[UserFeaturesDao],
          workspacesDao = env.get[WorkspacesDao],
          workspaceUserDao = env.get[WorkspaceUserDao],
          sessionsDao = env.get[SessionsDao],
          userSessionsDao = env.get[UserSessionsDao],
          verifyTokenDao = env.get[VerifyTokenDao]
        )
      )
    }
