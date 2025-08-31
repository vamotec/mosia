package app.mosia.infra.repository.impl

import app.mosia.core.configs.AppConfig
import app.mosia.domain.model.*
import app.mosia.domain.model.SessionResult.*
import app.mosia.infra.dao.*
import app.mosia.infra.dao.impl.DaoModuleImpl
import app.mosia.infra.repository.SessionRepo
import app.mosia.mapper.DomainMappers.toDomain
import zio.*

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

case class SessionRepoImpl(dao: DaoModule, configRef: Ref[AppConfig], ds: DataSource) extends SessionRepo:
  private val defaultTtrMillis: Long = 5 * 60 * 1000

  override def createSession(): Task[Sessions] =
    for
      db     <- dao.sessionsDao.create().provideEnvironment(ZEnvironment(ds))
      result <- toDomain(db)
    yield result

  override def getSession(sessionId: UUID): Task[Option[Sessions]] =
    for
      db     <- dao.sessionsDao.findFirst(sessionId).provideEnvironment(ZEnvironment(ds))
      result <- ZIO.foreach(db)(toDomain)
    yield result

  override def deleteSession(sessionId: UUID): Task[Long] =
    for
      count <- dao.sessionsDao.deleteMany(sessionId).provideEnvironment(ZEnvironment(ds))
      _     <-
        if (count > 0) ZIO.logInfo(s"Deleted session success by id: $sessionId")
        else ZIO.logWarning(s"Deleted session: $sessionId failed")
    yield count

  override def createOrRefreshUserSession(
    userId: UUID,
    sessionIdOpt: Option[UUID]
  ): Task[UserSessions] =
    for
      config    <- configRef.get
      ttlSeconds = config.auth.session.ttl
      expiresAt  = Instant.now().plusSeconds(ttlSeconds)
      sessionId <- sessionIdOpt match
                     case Some(sessionId) =>
                       dao.sessionsDao
                         .findFirst(sessionId)
                         .provideEnvironment(ZEnvironment(ds))
                         .flatMap:
                           case Some(_) => ZIO.succeed(sessionId)
                           case None    => createSession().map(_.id.value)
                     case None            =>
                       createSession().map(_.id.value)
      db        <- dao.userSessionsDao
                     .upsert(
                       sessionId = sessionId,
                       userId = userId,
                       expiresAt = expiresAt
                     )
                     .provideEnvironment(ZEnvironment(ds))
      result    <- toDomain(db)
    yield result

  override def refreshUserSessionIfNeeded(userSessions: UserSessions, ttr: Option[Duration]): Task[Option[Instant]] =
    val ttrMillis = ttr.fold(defaultTtrMillis)(_.toMillis)
    // 检查是否需要刷新会话
    if (userSessions.expiresAt.toInstant.toEpochMilli - java.time.Instant.now().toEpochMilli > ttrMillis)
      ZIO.succeed(Some(userSessions.createdAt.toInstant))
    else
      for
        config      <- configRef.get
        newExpiresAt = java.time.Instant.now().plusMillis(config.auth.session.ttl * 1000)
        _           <- dao.userSessionsDao.update(userSessions.id.value, newExpiresAt).provideEnvironment(ZEnvironment(ds))
      yield Some(newExpiresAt)

  override def findUserSessionsBySessionId(
    sessionId: UUID,
    include: Option[Boolean] = None
  ): Task[List[SessionResult]] =
    for
      sessions <- dao.userSessionsDao.findMany(sessionId).provideEnvironment(ZEnvironment(ds))
      result   <- ZIO.foreach(sessions): dbSession =>
                    for
                      session <- toDomain(dbSession)
                      dbUser  <- dao.usersDao
                                   .getUserById(dbSession.userId)
                                   .provideEnvironment(ZEnvironment(ds))
                                   .catchAll(e => ZIO.fail(new Exception(s"DB error: ${e.getMessage}", e)))
                      userOpt <- ZIO.foreach(dbUser)(toDomain)
                      conv    <- include match
                                   case Some(true) => ZIO.succeed(UserSessionWithUser(session = session, user = userOpt.get))
                                   case _          => ZIO.succeed(UserSessionWithOutUser(session = session))
                    yield conv
    yield result

  override def deleteUserSession(userId: UUID, sessionIdOpt: Option[UUID] = None): Task[Long] =
    sessionIdOpt match
      case Some(sessionId) =>
        for
          count <- dao.userSessionsDao.delete(userId, sessionId).provideEnvironment(ZEnvironment(ds))
          _     <-
            if (count > 0) ZIO.logInfo(s"Deleted user sessions success by userId: $userId and sessionId: $sessionId")
            else ZIO.logWarning(s"Deleted user sessions by userId: $userId and sessionId: $sessionId failed")
        yield count
      case None            =>
        for
          count <- dao.userSessionsDao.deleteByUser(userId).provideEnvironment(ZEnvironment(ds))
          _     <-
            if (count > 0) ZIO.logInfo(s"Deleted user sessions success by userId: $userId")
            else ZIO.logWarning(s"Deleted user sessions by userId: $userId failed")
        yield count

  override def cleanExpiredUserSessions(): Task[Long] =
    for
      count <- dao.userSessionsDao.clean().provideEnvironment(ZEnvironment(ds))
      _     <-
        if (count > 0) ZIO.logInfo(s"Cleaned $count expired user sessions")
        else ZIO.logInfo("No sessions record to clean")
    yield count

object SessionRepoImpl:
  def make: ZIO[Ref[AppConfig] & DaoModule & DataSource, Nothing, SessionRepo] =
    for
      dao       <- ZIO.service[DaoModule]
      configRef <- ZIO.service[Ref[AppConfig]]
      ds        <- ZIO.service[DataSource]
    yield new SessionRepoImpl(dao, configRef, ds)

  val live: ZLayer[DataSource & Ref[AppConfig], Throwable, SessionRepo] = DaoModuleImpl.layer >>> ZLayer.fromZIO(make)
