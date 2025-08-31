package app.mosia.infra.dao.impl

import app.mosia.infra.dao.*
import app.mosia.models.*
import io.getquill.*
import io.getquill.extras.InstantOps
import zio.*

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

case class UserSessionsDaoImpl(dataSource: DataSource) extends UserSessionsDao:
  import QuillContext.*

  override def findMany(sessionId: UUID): RIO[DataSource, List[DbUserSessions]] =
    val now              = Instant.now()
    inline def baseQuery = quote:
      DbUserSessions.schema
        .filter(s =>
          s.sessionId == lift(sessionId) &&
            sql"${s.expiresAt} IS NULL OR ${s.expiresAt} > ${lift(now)}".as[Boolean]
        )
        .sortBy(_.createdAt)(Ord.asc)

    run(baseQuery)

  override def update(id: UUID, expiresAt: Instant): RIO[DataSource, Long] =
    inline def queries = quote:
      DbUserSessions.schema
        .filter(s => s.id == lift(id))
        .update(_.expiresAt -> lift(expiresAt))

    run(queries)

  override def upsert(sessionId: UUID, userId: UUID, expiresAt: Instant): RIO[DataSource, DbUserSessions] =
    inline def queries = quote:
      DbUserSessions.schema
        .insert(
          _.userId    -> lift(userId),
          _.sessionId -> lift(sessionId),
          _.expiresAt -> lift(expiresAt)
        )
        .onConflictUpdate(_.sessionId, _.userId)((t, e) => t.expiresAt -> e.expiresAt)
        .returning(r => r)

    run(queries)

  override def delete(userId: UUID, sessionId: UUID): RIO[DataSource, Long] =
    inline def queries = quote:
      DbUserSessions.schema
        .filter(s => s.userId == lift(userId) && s.sessionId == lift(sessionId))
        .delete

    run(queries)

  override def clean(): RIO[DataSource, Long] =
    inline def queries = quote:
      DbUserSessions.schema
        .filter(_.expiresAt < lift(Instant.now()))
        .delete

    run(queries)

  override def deleteByUser(userId: UUID): RIO[DataSource, Long] =
    inline def queries = quote:
      DbUserSessions.schema
        .filter(_.userId == lift(userId))
        .delete

    run(queries)

object UserSessionsDaoImpl {
  val live: URLayer[DataSource, UserSessionsDao] = ZLayer.fromFunction(UserSessionsDaoImpl.apply _)
}
