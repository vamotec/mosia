package app.mosia.infra.dao

import app.mosia.models.DbUserSessions
import zio.*

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

trait UserSessionsDao {
  def findMany(sessionId: UUID): RIO[DataSource, List[DbUserSessions]]
  def update(id: UUID, expiresAt: Instant): RIO[DataSource, Long]
  def upsert(sessionId: UUID, userId: UUID, expiresAt: Instant): RIO[DataSource, DbUserSessions]
  def delete(userId: UUID, sessionId: UUID): RIO[DataSource, Long]
  def deleteByUser(userId: UUID): RIO[DataSource, Long]
  def clean(): RIO[DataSource, Long]
}
