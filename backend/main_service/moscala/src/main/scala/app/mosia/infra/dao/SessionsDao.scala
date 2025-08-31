package app.mosia.infra.dao

import app.mosia.models.DbMultipleUsersSessions
import zio.{ RIO, Task }

import java.util.UUID
import javax.sql.DataSource

trait SessionsDao {
  def create(): RIO[DataSource, DbMultipleUsersSessions]
  def findFirst(sessionId: UUID): RIO[DataSource, Option[DbMultipleUsersSessions]]
  def deleteMany(sessionId: UUID): RIO[DataSource, Long]
}
