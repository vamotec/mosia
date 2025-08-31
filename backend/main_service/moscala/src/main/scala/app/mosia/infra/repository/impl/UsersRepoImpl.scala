package app.mosia.infra.repository.impl

import app.mosia.application.dto.{ ConnectedAccountDto, CreateUserDto }
import app.mosia.core.configs.AppConfig
import app.mosia.core.errors.UserFriendlyError.*
import app.mosia.infra.eventbus.EventBus
import app.mosia.infra.events.*
import app.mosia.domain.model.Email.Email
import app.mosia.domain.model.*
import app.mosia.domain.model.input.{ ConnectedAccountInput, UsersInput }
import app.mosia.domain.model.update.UsersUpdate
import app.mosia.infra.dao.DaoModule
import app.mosia.infra.dao.impl.DaoModuleImpl
import app.mosia.infra.dao.impl.QuillContext.transaction
import app.mosia.infra.oauth.Types.OAuthProviderName
import app.mosia.infra.repository.{ UsersRepo, WorkspaceUserRepo }
import app.mosia.infra.workspace.Permission
import app.mosia.mapper.DomainMappers.*
import zio.*

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

case class UsersRepoImpl(
  dao: DaoModule,
  workspaceUserRepo: WorkspaceUserRepo,
  event: EventBus,
  ds: DataSource
) extends UsersRepo:
  override def getUserById(id: UUID): Task[Option[Users]] =
    for
      db   <- dao.usersDao.getUserById(id).provideEnvironment(ZEnvironment(ds))
      user <- ZIO.foreach(db)(toDomain)
    yield user

  override def getUserByEmail(email: Email): Task[Option[Users]] =
    for
      db     <- dao.usersDao.getUserByEmail(email.value).provideEnvironment(ZEnvironment(ds))
      result <- ZIO.foreach(db)(toDomain)
    yield result

  override def signIn(email: Email, password: String): Task[Users] =
    for
      user <- getUserByEmail(email).flatMap {
                case Some(u) => ZIO.succeed(u)
                case None    => ZIO.fail(WrongSignInCredentials(email.value))
              }
      _    <- ZIO.fail(WrongSignInCredentials(email.value)).unless(user.validatePassword(password))
    yield user

  override def create(email: Email, name: Option[String], password: Option[String]): Task[Users] =
    for
      // 查重
      userOpt <- getUserByEmail(email)
      _       <- ZIO.when(userOpt.isDefined)(ZIO.fail(EmailAlreadyUsed(email.value)))

      // 构建 UsersInput（领域对象）
      usersInput <- ZIO.attempt {
                      password match {
                        case Some(pwd) =>
                          UsersInput.withPassword(
                            email = email.value,
                            plainPassword = pwd,
                            name = name.getOrElse(email.username)
                          )
                        case None      =>
                          UsersInput.oauth(
                            email = email.value,
                            name = name.getOrElse(email.username)
                          )
                      }
                    }

      // 持久化（NewUser → DB entity）
      dbUser <- dao.usersDao.create(usersInput).provideEnvironment(ZEnvironment(ds))
      user   <- dbUser.toDomain

      // 发布事件
      _ <- event.emit("user.created", UserUpdatedEvent(user.id.value.toString))

      // 日志
      _ <- ZIO.logDebug(s"User [${user.id.value}] created with email [${user.email.value}]")
    yield user

  override def update(id: UUID, data: UsersUpdate): RIO[DataSource, Users] = transaction[DataSource, Users]:
    for
      // 获取现有用户
      existingUser <- getUserById(id).someOrFail(UserNotFound(id.toString))

      // 密码处理
      updatedPassword <- data.password match
                           case Some(newPwd) =>
                             ZIO.succeed(Option(newPwd))
                           case None         =>
                             ZIO.succeed(existingUser.passwordHash) // 保持不变

      // 应用更新逻辑（由领域模型负责）
      updatedUser = existingUser.applyUpdate(data).copy(passwordHash = updatedPassword)

      // 持久化
      dbUser <- dao.usersDao.updateUser(id, updatedUser)
      user   <- dbUser.toDomain

      // 日志 & 事件
      _ <- ZIO.logInfo(s"User [${user.id.value}] updated")
      _ <- event.emit("user.updated", UserUpdatedEvent(user.id.value.toString))
    yield user

  override def delete(id: UUID): Task[Users] =
    for
      ownedWorkspaces <- workspaceUserRepo.getUserActiveRoles(id, Some(Permission.Owner))
      workspaceIds     = ownedWorkspaces.map(_.workspaceId)
      user            <- getUserById(id).someOrFail(new Exception(s"User with id $id not found"))
      _               <- dao.usersDao.deleteUser(id).provideEnvironment(ZEnvironment(ds))
      _               <- event.emit("user.deleted", UserEvent.Deleted(user, workspaceIds))
    yield user

  override def count(): Task[Long] = dao.usersDao.count().provideEnvironment(ZEnvironment(ds))

  override def enable(id: UUID): Task[Users] =
    update(
      id = id,
      data = UsersUpdate(disabled = Some(false))
    ).provideEnvironment(ZEnvironment(ds))

  /**
   * Mark an existing user or create a new one as registered and email verified.
   *
   * When user created by others invitation, we will leave it as unregistered.
   */
  override def fullfill(email: Email, name: Option[String], password: Option[String]): Task[Users] =
    for
      existingUser <- getUserByEmail(email)
      now           = Instant.now()
      result       <- existingUser match
                        case None       => create(email, name, password)
                        case Some(user) =>
                          val updated = UsersUpdate(
                            registered = if (user.registered) None else Some(true),
                            emailVerified = if (user.emailVerified.isDefined) None else Some(true)
                          )
                          update(user.id.value, updated).provideEnvironment(ZEnvironment(ds))
    yield result

  override def pagination(
    skip: Int = 0,
    take: Int = 20,
    afterOpt: Option[Instant]
  ): Task[List[Users]] =
    for
      after  <- afterOpt match
                  case Some(time) => ZIO.succeed(time)
                  case None       => ZIO.succeed(Instant.EPOCH)
      dbList <- dao.usersDao.findAfter(after, skip, take).provideEnvironment(ZEnvironment(ds))
      list   <- toDomainList(dbList)
    yield list

  override def createConnectedAccount(data: ConnectedAccountInput): Task[Accounts] =
    for
      db      <- dao.userConnectedAccountsDao.create(data).provideEnvironment(ZEnvironment(ds))
      account <- toDomain(db)
    yield account

  override def getConnectedAccount(
    provider: OAuthProviderName,
    providerAccountId: String
  ): Task[Option[Accounts]] =
    for
      db       <-
        dao.userConnectedAccountsDao.findUser(provider.toString, providerAccountId).provideEnvironment(ZEnvironment(ds))
      accounts <- ZIO.foreach(db)(toDomain)
    yield accounts

  override def updateConnectedAccount(id: UUID, data: ConnectedAccountInput): Task[Accounts] =
    for
      db       <- dao.userConnectedAccountsDao.update(id, data).provideEnvironment(ZEnvironment(ds))
      accounts <- toDomain(db)
    yield accounts

  override def deleteConnectedAccount(id: UUID): Task[Long] =
    dao.userConnectedAccountsDao.delete(id).provideEnvironment(ZEnvironment(ds))

object UsersRepoImpl:
  def make: ZIO[EventBus & WorkspaceUserRepo & DaoModule & DataSource, Nothing, UsersRepo] =
    for
      dao   <- ZIO.service[DaoModule]
      repo  <- ZIO.service[WorkspaceUserRepo]
      event <- ZIO.service[EventBus]
      ds    <- ZIO.service[DataSource]
    yield new UsersRepoImpl(dao, repo, event, ds)

  val env: ZLayer[DataSource & EventBus, Throwable, DaoModule & WorkspaceUserRepo] =
    DaoModuleImpl.layer ++ WorkspaceUserRepoImpl.live

  val live: ZLayer[DataSource & Ref[AppConfig] & EventBus, Throwable, UsersRepo] = env >>> ZLayer.fromZIO(make)
