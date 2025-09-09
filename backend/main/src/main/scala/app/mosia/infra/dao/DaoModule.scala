package app.mosia.infra.dao

trait DaoModule:
  def userConnectedAccountsDao: UserConnectedAccountsDao
  def usersDao: UsersDao
  def workspacesDao: WorkspacesDao
  def workspaceUserDao: WorkspaceUserDao
  def sessionsDao: SessionsDao
  def userSessionsDao: UserSessionsDao
  def featuresDao: FeaturesDao
  def userFeaturesDao: UserFeaturesDao
  def configsDao: ConfigsDao
  def verifyTokenDao: VerifyTokenDao
