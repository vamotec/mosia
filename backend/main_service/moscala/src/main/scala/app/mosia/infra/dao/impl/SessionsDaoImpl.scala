package app.mosia.infra.dao.impl

import app.mosia.infra.dao.SessionsDao
import app.mosia.models.DbMultipleUsersSessions
import io.getquill.*
import zio.*

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

case class SessionsDaoImpl(dataSource: DataSource) extends SessionsDao:
  import QuillContext.{ *, given }

  override def create(): RIO[DataSource, DbMultipleUsersSessions] =
    inline def queries = quote:
      sql"""
        INSERT INTO multiple_users_sessions DEFAULT VALUES 
        RETURNING id, created_at
      """.as[Query[DbMultipleUsersSessions]]

    run(queries).map(_.head)

  override def findFirst(sessionId: UUID): RIO[DataSource, Option[DbMultipleUsersSessions]] =
    inline def queries = quote:
      DbMultipleUsersSessions.schema
        .filter(_.id == lift(sessionId))

    run(queries).map(_.headOption)

  override def deleteMany(sessionId: UUID): RIO[DataSource, Long] =
    inline def queries = quote:
      DbMultipleUsersSessions.schema
        .filter(_.id == lift(sessionId))
        .delete

    run(queries)

object SessionsDaoImpl:
  val live: URLayer[DataSource, SessionsDao] = ZLayer.fromFunction(SessionsDaoImpl.apply _)
