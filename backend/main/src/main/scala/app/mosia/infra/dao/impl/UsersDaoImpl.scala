package app.mosia.infra.dao.impl

import app.mosia.domain.model.*
import app.mosia.domain.model.input.UsersInput
import app.mosia.domain.model.update.UsersUpdate
import app.mosia.infra.dao.UsersDao
import app.mosia.models.DbUsers
import io.getquill.*
import io.getquill.extras.InstantOps
import zio.{ RIO, URLayer, ZIO, ZLayer }

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import scala.math.Ordered.orderingToOrdered

case class UsersDaoImpl(dataSource: DataSource) extends UsersDao:
  import QuillContext.*

  override def getUserById(id: UUID): RIO[DataSource, Option[DbUsers]] =
    inline def queries = quote:
      DbUsers.schema
        .filter(_.id == lift(id))

    run(queries).map(_.headOption)

  override def getUserByEmail(email: String): RIO[DataSource, Option[DbUsers]] =
    inline def queries = quote:
      DbUsers.schema
        .filter(_.email == lift(email))

    run(queries).map(_.headOption)

  override def updateUserAvatar(id: UUID, avatarUrl: String): RIO[DataSource, Long] =
    val avatarOpt: Option[String] = Option(avatarUrl)
    inline def queries            = quote:
      DbUsers.schema
        .filter(_.id == lift(id))
        .update(_.avatarUrl -> lift(avatarOpt))

    run(queries)

  override def updateUser(id: UUID, user: Users): RIO[DataSource, DbUsers] =
    inline def queries = quote:
      DbUsers.schema
        .filter(_.id == lift(id))
        .update(
          _.name          -> lift(user.name),
          _.avatarUrl     -> lift(user.avatarUrl),
          _.passwordHash  -> lift(user.passwordHash.map(_.hash)),
          _.registered    -> lift(user.registered),
          _.disabled      -> lift(user.disabled),
          _.updatedAt     -> lift(user.updatedAt.toInstant),
          _.emailVerified -> lift(user.emailVerified.map(_.toInstant))
        )
        .returning(r => r)

    run(queries)

  override def deleteUser(id: UUID): RIO[DataSource, Long] =
    inline def queries = quote:
      DbUsers.schema
        .filter(_.id == lift(id))
        .delete

    run(queries)

  override def create(data: UsersInput): RIO[DataSource, DbUsers] =
    inline def queries = quote:
      DbUsers.schema
        .insert(
          _.name         -> lift(data.name),
          _.email        -> lift(data.email),
          _.passwordHash -> lift(data.password.map(_.hash))
        )
        .returning(r => r)

    run(queries)

  override def findAfter(after: Instant, offset: Int, take: Int): RIO[DataSource, List[DbUsers]] =
    inline def queries = quote:
      DbUsers.schema
        .filter(_.createdAt > lift(after))
        .sortBy(_.createdAt)(Ord.asc)
        .drop(lift(offset))
        .take(lift(take))

    run(queries)

  override def count(): RIO[DataSource, Long] =
    inline def queries = quote:
      DbUsers.schema.size

    run(queries)

object UsersDaoImpl:
  val live: URLayer[DataSource, UsersDao] = ZLayer.fromFunction(UsersDaoImpl.apply _)
