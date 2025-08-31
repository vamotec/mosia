package app.mosia.infra.repository

import app.mosia.domain.model.{ SessionResult, Sessions, UserSessions }
import zio.*

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

trait SessionRepo:
  def createSession(): Task[Sessions]
  def getSession(sessionId: UUID): Task[Option[Sessions]]
  def deleteSession(sessionId: UUID): Task[Long]
  def createOrRefreshUserSession(userId: UUID, sessionIdOpt: Option[UUID]): Task[UserSessions]
  def refreshUserSessionIfNeeded(userSessions: UserSessions, ttr: Option[Duration]): Task[Option[Instant]]
  def findUserSessionsBySessionId(sessionId: UUID, include: Option[Boolean] = None): Task[List[SessionResult]]
  def deleteUserSession(userId: UUID, sessionId: Option[UUID] = None): Task[Long]
  def cleanExpiredUserSessions(): Task[Long]
