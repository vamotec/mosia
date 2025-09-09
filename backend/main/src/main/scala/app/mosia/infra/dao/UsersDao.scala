package app.mosia.infra.dao

import app.mosia.domain.model.*
import app.mosia.domain.model.input.UsersInput
import app.mosia.domain.model.update.UsersUpdate
import app.mosia.models.DbUsers
import zio.*

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

trait UsersDao:
  def getUserById(id: UUID): RIO[DataSource, Option[DbUsers]]
  def getUserByEmail(email: String): RIO[DataSource, Option[DbUsers]]
  def create(data: UsersInput): RIO[DataSource, DbUsers]
  def findAfter(after: Instant, offset: Int, take: Int): RIO[DataSource, List[DbUsers]]
  def updateUserAvatar(id: UUID, avatarUrl: String): RIO[DataSource, Long]
  def updateUser(id: UUID, user: Users): RIO[DataSource, DbUsers]
  def deleteUser(id: UUID): RIO[DataSource, Long]
  def count(): RIO[DataSource, Long]
