package app.mosia.infra.dao.impl

import app.mosia.domain.model.input.ConnectedAccountInput
import app.mosia.infra.dao.{ UserConnectedAccountsDao, WorkspacesDao }
import app.mosia.models.DbUserConnectedAccounts
import io.getquill.*
import zio.{ RIO, URLayer, ZIO, ZLayer }

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

case class UserConnectedAccountsDaoImpl(dataSource: DataSource) extends UserConnectedAccountsDao:
  import QuillContext.*

  override def update(id: UUID, data: ConnectedAccountInput): RIO[DataSource, DbUserConnectedAccounts] =
    inline def queries = quote:
      DbUserConnectedAccounts.schema
        .filter(_.id == lift(id))
        .update(
          _.userId -> lift(data.userId)
        )
        .returning(r => r)

    run(queries)

  override def create(data: ConnectedAccountInput): RIO[DataSource, DbUserConnectedAccounts] =
    inline def queries = quote:
      DbUserConnectedAccounts.schema
        .insert(
          _.userId            -> lift(data.userId),
          _.providerAccountId -> lift(data.providerAccountId),
          _.provider          -> lift(data.provider),
          _.scope             -> lift(data.scope),
          _.expiresAt         -> lift(data.expiresAt),
          _.updatedAt         -> lift(Instant.now()),
          _.accessToken       -> lift(data.accessToken),
          _.refreshToken      -> lift(data.refreshToken)
        )
        .returning(r => r)

    run(queries)

  override def findUser(provider: String, providerAccountId: String): RIO[DataSource, Option[DbUserConnectedAccounts]] =
    inline def queries = quote:
      DbUserConnectedAccounts.schema
        .filter(row => row.provider == lift(provider) && row.providerAccountId == lift(providerAccountId))

    run(queries).map(_.headOption)

  override def delete(id: UUID): RIO[DataSource, Long] =
    inline def queries = quote:
      DbUserConnectedAccounts.schema
        .filter(_.id == lift(id))
        .delete

    run(queries)

object UserConnectedAccountsDaoImpl:
  val live: URLayer[DataSource, UserConnectedAccountsDao] = ZLayer.fromFunction(UserConnectedAccountsDaoImpl.apply _)
