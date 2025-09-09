package app.mosia.infra.dao

import app.mosia.domain.model.input.ConnectedAccountInput
import app.mosia.models.DbUserConnectedAccounts
import zio.RIO

import java.util.UUID
import javax.sql.DataSource

trait UserConnectedAccountsDao {
  def create(data: ConnectedAccountInput): RIO[DataSource, DbUserConnectedAccounts]
  def update(id: UUID, data: ConnectedAccountInput): RIO[DataSource, DbUserConnectedAccounts]
  def findUser(provider: String, providerAccountId: String): RIO[DataSource, Option[DbUserConnectedAccounts]]
  def delete(id: UUID): RIO[DataSource, Long]
}
