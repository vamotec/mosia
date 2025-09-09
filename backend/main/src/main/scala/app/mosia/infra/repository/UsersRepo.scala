package app.mosia.infra.repository

import app.mosia.application.dto.{ CreateUserDto, UserResponseDto }
import app.mosia.domain.model.*
import app.mosia.domain.model.Email.Email
import app.mosia.domain.model.input.ConnectedAccountInput
import app.mosia.domain.model.update.UsersUpdate
import app.mosia.infra.oauth.Types.OAuthProviderName
import zio.{ RIO, Task }

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

trait UsersRepo {
  def getUserById(id: UUID): Task[Option[Users]]
//  def getWorkspaceUser(id: UUID): Task[Option[WorkspaceUserType]]
//  def getWorkspaceUsers(ids: List[UUID]): Task[List[WorkspaceUserType]]
  def getUserByEmail(email: Email): Task[Option[Users]]
  def signIn(email: Email, password: String): Task[Users]
  def create(email: Email, name: Option[String], password: Option[String]): Task[Users]
  def update(id: UUID, data: UsersUpdate): RIO[DataSource, Users]
  def delete(id: UUID): Task[Users]
  def enable(id: UUID): Task[Users]
  def count(): Task[Long]
  def fullfill(email: Email, name: Option[String], password: Option[String]): Task[Users]
  def pagination(
    skip: Int = 0,
    take: Int = 20,
    after: Option[Instant]
  ): Task[List[Users]]
  def createConnectedAccount(data: ConnectedAccountInput): Task[Accounts]
  def getConnectedAccount(provider: OAuthProviderName, providerAccountId: String): Task[Option[Accounts]]
  def updateConnectedAccount(id: UUID, data: ConnectedAccountInput): Task[Accounts]
  def deleteConnectedAccount(id: UUID): Task[Long]
}
