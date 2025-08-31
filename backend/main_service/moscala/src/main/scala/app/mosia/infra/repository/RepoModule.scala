package app.mosia.infra.repository

trait RepoModule:
  def usersRepo: UsersRepo
  def sessionRepo: SessionRepo
  def workspaceUserRepo: WorkspaceUserRepo
  def configsRepo: ConfigsRepo
  def userFeaturesRepo: UserFeaturesRepo
  def featuresRepo: FeaturesRepo
  def verifyTokenRepo: VerifyTokenRepo
