package app.mosia.infra.repository.impl

import app.mosia.core.configs.AppConfig
import app.mosia.core.errors.UserFriendlyError.*
import app.mosia.infra.eventbus.{ EventBus, EventBusImpl }
import app.mosia.infra.events.WorkspaceMembersEvent.RoleChanged
import app.mosia.domain.model.*
import app.mosia.domain.model.input.WorkspaceUserInput
import app.mosia.infra.dao.DaoModule
import app.mosia.infra.dao.impl.DaoModuleImpl
import app.mosia.infra.dao.impl.QuillContext.transaction
import app.mosia.infra.events.WorkspaceEvent.*
import app.mosia.infra.repository.WorkspaceUserRepo
import app.mosia.infra.workspace.*
import app.mosia.infra.workspace.Permission.*
import app.mosia.mapper.DomainMappers.*
import app.mosia.models.DbWorkspaceUserPermissions
import zio.*
import zio.kafka.consumer.Consumer
import zio.kafka.producer.Producer

import java.util.UUID
import javax.sql.DataSource

case class WorkspaceUserRepoImpl(dao: DaoModule, eventBus: EventBus, ds: DataSource) extends WorkspaceUserRepo:
  /**
   * Set or update the [Owner] of a workspace.
   * The old [Owner] will   be changed to [Admin] if there is already an [Owner].
   */
  override def setOwner(workspaceId: UUID, userId: UUID): RIO[DataSource, Unit] =
    transaction[DataSource, Unit]:
      for
        oldOwnerOpt <- dao.workspaceUserDao.findOwner(workspaceId)
        _           <- oldOwnerOpt match
                         case Some(oldOwner) =>
                           for
                             newOwnerOpt <- dao.workspaceUserDao.findUser(workspaceId, userId)
                             newOwner    <-
                               ZIO
                                 .fromOption(newOwnerOpt)
                                 .mapError(_ => new RuntimeException("User not found"))
                                 .filterOrFail(_.status == WorkspaceMemberStatus.Accepted.toString)(NewOwnerIsNotActiveMember())

                             _ <- dao.workspaceUserDao.updateRole(oldOwner.id, Permission.Admin.toInt)
                             _ <- dao.workspaceUserDao.updateRole(newOwner.id, Permission.Owner.toInt)
                             _ <- eventBus.emit[OwnerChanged](
                                    "workspace.owner.changed",
                                    OwnerChanged(workspaceId, oldOwner.userId, userId)
                                  )
                             _ <-
                               ZIO.logInfo(s"Transfer workspace owner of [$workspaceId] from [${oldOwner.userId}] to [$userId]")
                           yield ()

                         case None =>
                           for
                             _ <- dao.workspaceUserDao.create(
                                    WorkspaceUserInput(
                                      workspaceId = workspaceId,
                                      userId = userId,
                                      `type` = Permission.Owner.toInt,
                                      status = WorkspaceMemberStatus.Accepted.toString,
                                      inviterId = None,
                                      source = WorkspaceMemberSource.Email.toString
                                    )
                                  )
                             _ <- ZIO.logInfo(s"Set workspace owner of [$workspaceId] to [$userId]")
                           yield ()
      yield ()

  /**
   * Set or update the Role of a user in a workspace.=
   *
   * NOTE: do not use this method to set the [Owner] of a workspace. Use [[setOwner]] instead.
   */
  override def set(
    workspaceId: UUID,
    userId: UUID,
    role: Permission,
    defaultData: WorkspaceDataType
  ): RIO[DataSource, Unit] = transaction[DataSource, Unit]:
    role match
      case Owner => ZIO.fail(new RuntimeException("Cannot grant Owner role of a workspace to a user."))
      case _     =>
        for
          oldRoleOpt <- get(workspaceId, userId)
          _          <- oldRoleOpt match
                          case Some(oldRole) =>
                            if (oldRole.`type` == role.toInt) ZIO.succeed(oldRole)
                            else
                              for
                                _       <- dao.workspaceUserDao.updateRole(oldRole.id.value, role.toInt)
                                newRole <- dao.workspaceUserDao
                                             .findUnique(oldRole.id.value)
                                             .some
                                             .orElseFail(new Exception("Role not found"))
                                _       <- ZIO.when(oldRole.status == WorkspaceMemberStatus.Accepted.toString)(
                                             eventBus.emit(
                                               "workspace.members.roleChanged",
                                               RoleChanged(workspaceId, userId, Permission.fromInt(newRole.`type`))
                                             )
                                           )
                              yield ()
                          case None          =>
                            dao.workspaceUserDao
                              .create(
                                WorkspaceUserInput(
                                  workspaceId = workspaceId,
                                  userId = userId,
                                  `type` = role.toInt,
                                  status = defaultData.status.getOrElse(WorkspaceMemberStatus.Pending).toString,
                                  inviterId = defaultData.inviterId,
                                  source = defaultData.source.getOrElse(WorkspaceMemberSource.Email).toString
                                )
                              )
        yield ()

  override def setStatus(
    workspaceId: UUID,
    userId: UUID,
    defaultData: WorkspaceDataType
  ): Task[Unit] =
    for
      dbOpt <- dao.workspaceUserDao.findUser(workspaceId, userId).provideEnvironment(ZEnvironment(ds))
      _     <- dbOpt match
                 case Some(db) =>
                   dao.workspaceUserDao
                     .update(
                       WorkspaceUserInput(
                         workspaceId = workspaceId,
                         userId = userId,
                         `type` = db.`type`,
                         status = defaultData.status.getOrElse(WorkspaceMemberStatus.valueOf(db.status)).toString,
                         inviterId = defaultData.inviterId,
                         source = db.source
                       )
                     )
                     .provideEnvironment(ZEnvironment(ds))
                 case None     => ZIO.fail(new RuntimeException("no record to update"))
    yield ()

  override def delete(workspaceId: UUID, userId: UUID): Task[Long] =
    dao.workspaceUserDao.delete(workspaceId, userId).provideEnvironment(ZEnvironment(ds))

  override def deleteByUserId(userId: UUID): Task[Long] =
    dao.workspaceUserDao.deleteMany(userId).provideEnvironment(ZEnvironment(ds))

  override def get(workspaceId: UUID, userId: UUID): Task[Option[WorkspaceUser]] =
    for
      dbOpt  <- dao.workspaceUserDao.findUser(workspaceId, userId).provideEnvironment(ZEnvironment(ds))
      result <- ZIO.foreach(dbOpt)(toDomain)
    yield result

  override def getById(id: UUID): Task[Option[WorkspaceUser]] =
    for
      dbOpt  <- dao.workspaceUserDao.findUnique(id).provideEnvironment(ZEnvironment(ds))
      result <- ZIO.foreach(dbOpt)(toDomain)
    yield result

  /**
   * Get the **accepted** Role of a user in a workspace.
   */
  override def getActive(workspaceId: UUID, userId: UUID): Task[Option[WorkspaceUser]] =
    for
      dbOpt  <- dao.workspaceUserDao.findActive(workspaceId, userId).provideEnvironment(ZEnvironment(ds))
      result <- ZIO.foreach(dbOpt)(toDomain)
    yield result

  override def getOwner(workspaceId: UUID): Task[Option[Users]] =
    for
      dbOpt   <- dao.workspaceUserDao.findOwner(workspaceId).provideEnvironment(ZEnvironment(ds))
      userOpt <- dbOpt match
                   case Some(db) => dao.usersDao.getUserById(db.userId).provideEnvironment(ZEnvironment(ds))
                   case None     => ZIO.fail(new RuntimeException("Workspace owner not found"))
      user    <- ZIO.foreach(userOpt)(toDomain)
    yield user

  override def getAdmins(workspaceId: UUID): Task[List[Users]] =
    for
      dbList      <- dao.workspaceUserDao.findAdmin(workspaceId).provideEnvironment(ZEnvironment(ds))
      userListOpt <- ZIO.foreach(dbList) { db =>
                       dao.usersDao.getUserById(db.userId).provideEnvironment(ZEnvironment(ds))
                     }
      dbUsers      = userListOpt.flatten
      userList    <- toDomainList(dbUsers)
    yield userList

  override def count(workspaceId: UUID): Task[Long] =
    dao.workspaceUserDao.count(workspaceId).provideEnvironment(ZEnvironment(ds))

  /**
   * Get the number of users those in the status should be charged in billing system in a workspace. input UnderReview
   */
  override def statusCount(workspaceId: UUID, status: WorkspaceMemberStatus): Task[Long] =
    dao.workspaceUserDao
      .countByStatus(workspaceId, status.toString)
      .provideEnvironment(ZEnvironment(ds))

  override def insufficientSeatMemberCount(workspaceId: UUID): Task[Long] =
    dao.workspaceUserDao
      .countByStatus(workspaceId, WorkspaceMemberStatus.NeedMoreSeat.toString)
      .provideEnvironment(ZEnvironment(ds))

  override def getUserActiveRoles(userId: UUID, role: Option[Permission]): Task[List[WorkspaceUser]] =
    role match
      case Some(r) =>
        for
          dbList <- dao.workspaceUserDao.findByUserRole(userId, r.toInt).provideEnvironment(ZEnvironment(ds))
          list   <- toDomainList(dbList)
        yield list
      case None    =>
        for
          dbList <- dao.workspaceUserDao.findByUser(userId).provideEnvironment(ZEnvironment(ds))
          list   <- toDomainList(dbList)
        yield list

  override def paginate(workspaceId: UUID, pagination: PaginationInput): Task[List[(WorkspaceUser, Users)]] =
    pagination.after match
      case Some(a) =>
        for
          db     <- dao.workspaceUserDao
                      .paginateAfter(workspaceId, a, pagination.first.getOrElse(10), pagination.offset.getOrElse(0))
                      .provideEnvironment(ZEnvironment(ds))
          result <- ZIO.foreach(db) { case (dbPerm, dbUser) =>
                      for
                        workspaceUser <- toDomain(dbPerm)
                        user          <- toDomain(dbUser)
                      yield (workspaceUser, user)
                    }
        yield result
      case None    =>
        for
          db     <- dao.workspaceUserDao
                      .paginate(workspaceId, pagination.first.getOrElse(10), pagination.offset.getOrElse(0))
                      .provideEnvironment(ZEnvironment(ds))
          result <- ZIO.foreach(db) { case (dbPerm, dbUser) =>
                      for
                        workspaceUser <- toDomain(dbPerm)
                        user          <- toDomain(dbUser)
                      yield (workspaceUser, user)
                    }
        yield result

  override def search(
    workspaceId: UUID,
    query: String,
    pagination: PaginationInput
  ): Task[List[(WorkspaceUser, Users)]] =
    for
      db     <-
        dao.workspaceUserDao
          .search(workspaceId, query, pagination.after, pagination.first.getOrElse(10), pagination.offset.getOrElse(0))
          .provideEnvironment(ZEnvironment(ds))
      result <- ZIO.foreach(db) { case (dbPerm, dbUser) =>
                  for
                    workspaceUser <- toDomain(dbPerm)
                    user          <- toDomain(dbUser)
                  yield (workspaceUser, user)
                }
    yield result

  override def allocateSeats(workspaceId: UUID, limit: Long): RIO[DataSource, List[WorkspaceUser]] =
    transaction[DataSource, List[WorkspaceUser]]:
      for
        // 统计已使用的座位数
        accepted <- statusCount(workspaceId, WorkspaceMemberStatus.Accepted)
        pending  <- statusCount(workspaceId, WorkspaceMemberStatus.Pending)

        result <-
          if (limit <= (accepted + pending))
            // 如果座位不足，返回空列表
            ZIO.succeed(List.empty[WorkspaceUser])
          else
            for
              // 查询待分配的成员
              allocateLimit <- dao.workspaceUserDao
                                 .tobeAllocate(workspaceId, limit - (accepted + pending))
              // 按 source 分组
              groups         = allocateLimit
                                 .groupBy(_.source)
                                 .asInstanceOf[Map[WorkspaceMemberSource, List[DbWorkspaceUserPermissions]]]

              // 更新 Email 来源成员为 Pending
              _ <- groups.get(WorkspaceMemberSource.Email).fold(ZIO.unit) { emailMembers =>
                     ZIO.foreach(emailMembers)(member =>
                       dao.workspaceUserDao.updateStatus(member.id, WorkspaceMemberStatus.Pending.toString)
                     )
                   }
              // 更新 Link 来源成员为 Accepted
              _ <- groups.get(WorkspaceMemberSource.Link).fold(ZIO.unit) { emailMembers =>
                     ZIO.foreach(emailMembers)(member =>
                       dao.workspaceUserDao.updateStatus(member.id, WorkspaceMemberStatus.Accepted.toString)
                     )
                   }

              // 将剩余的 AllocatingSeat 更新为 NeedMoreSeat
              allocate <- dao.workspaceUserDao
                            .findByStatus(workspaceId, WorkspaceMemberStatus.AllocatingSeat.toString)

              _        <- ZIO.foreach(allocate) { a =>
                            dao.workspaceUserDao.updateStatus(a.id, WorkspaceMemberStatus.NeedMoreSeat.toString)
                          }
              // 返回 Email 来源的成员（如果存在）
              dbReturn <- ZIO.succeed(groups.getOrElse(WorkspaceMemberSource.Email, List.empty))
              re       <- toDomainList(dbReturn)
            yield re
      yield result

object WorkspaceUserRepoImpl:
  def make: ZIO[EventBus & DaoModule & DataSource, Nothing, WorkspaceUserRepo] =
    for
      dao      <- ZIO.service[DaoModule]
      eventBus <- ZIO.service[EventBus]
      ds       <- ZIO.service[DataSource]
    yield new WorkspaceUserRepoImpl(dao, eventBus, ds)

  val live: ZLayer[DataSource & EventBus, Throwable, WorkspaceUserRepo] = DaoModuleImpl.layer >>> ZLayer.fromZIO(make)
