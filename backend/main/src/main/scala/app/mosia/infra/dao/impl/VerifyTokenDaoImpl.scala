package app.mosia.infra.dao.impl

import app.mosia.infra.dao.VerifyTokenDao
import app.mosia.models.DbVerifyTokens
import app.mosia.domain.model.input.VerifyTokenInput
import io.getquill.*
import io.getquill.extras.InstantOps
import zio.{ RIO, Task, URLayer, ZIO, ZLayer }

import java.util.UUID
import javax.sql.DataSource

case class VerifyTokenDaoImpl(dataSource: DataSource) extends VerifyTokenDao:
  import QuillContext.*

  override def create(data: VerifyTokenInput): RIO[DataSource, Long] =
    inline def queries = quote:
      DbVerifyTokens.schema
        .insert(
          _.token      -> lift(data.token),
          _.`type`     -> lift(data.tokenType.toInt),
          _.expiresAt  -> lift(data.expiresAt),
          _.credential -> lift(data.credential)
        )

    run(queries)

  def findUnique(token: UUID, `type`: Int): RIO[DataSource, Option[DbVerifyTokens]] =
    inline def queries = quote:
      DbVerifyTokens.schema
        .filter(_.token == lift(token))
        .filter(_.`type` == lift(`type`))

    run(queries).map(_.headOption)

  override def deleteMany(token: UUID, `type`: Int): RIO[DataSource, Long] =
    inline def queries = quote:
      DbVerifyTokens.schema
        .filter(_.`type` == lift(`type`))
        .filter(_.token == lift(token))
        .delete

    run(queries)

  override def deleteExpired(): RIO[DataSource, Long] =
    inline def queries = quote:
      DbVerifyTokens.schema
        .filter(_.expiresAt <= lift(java.time.Instant.now()))
        .delete

    run(queries)

object VerifyTokenDaoImpl:
  val live: URLayer[DataSource, VerifyTokenDao] = ZLayer.fromFunction(VerifyTokenDaoImpl.apply _)
