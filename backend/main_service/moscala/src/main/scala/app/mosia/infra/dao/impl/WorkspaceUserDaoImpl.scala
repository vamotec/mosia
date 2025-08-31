package app.mosia.infra.dao.impl

import app.mosia.infra.dao.WorkspaceUserDao
import app.mosia.domain.model.*
import app.mosia.domain.model.input.WorkspaceUserInput
import app.mosia.infra.workspace.*
import app.mosia.models.{ DbUsers, DbWorkspaceUserPermissions }
import io.getquill.*
import io.getquill.extras.InstantOps
import zio.{ RIO, URLayer, ZIO, ZLayer }

import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

case class WorkspaceUserDaoImpl(dataSource: DataSource) extends WorkspaceUserDao:
  import QuillContext.*

  override def findOwner(workspaceId: UUID): RIO[DataSource, Option[DbWorkspaceUserPermissions]] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema.filter(row => row.workspaceId == lift(workspaceId) && row.`type` == lift(99))

    run(queries).map(_.headOption)

  override def findUser(workspaceId: UUID, userId: UUID): RIO[DataSource, Option[DbWorkspaceUserPermissions]] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .filter(row => row.workspaceId == lift(workspaceId) && row.userId == lift(userId))

    run(queries).map(_.headOption)

  override def updateRole(id: UUID, role: Int): RIO[DataSource, Long] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .filter(_.id == lift(id))
        .update(_.`type` -> lift(role))

    run(queries)

  override def updateStatus(id: UUID, status: String): RIO[DataSource, Long] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .filter(_.id == lift(id))
        .update(_.status -> lift(status))

    run(queries)

  override def create(data: WorkspaceUserInput): RIO[DataSource, UUID] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .insert(
          _.workspaceId -> lift(data.workspaceId),
          _.userId      -> lift(data.userId),
          _.status      -> lift(data.status),
          _.`type`      -> lift(data.`type`),
          _.source      -> lift(data.source),
          _.inviterId   -> lift(data.inviterId)
        )
        .returning(_.id)

    run(queries)

  override def update(data: WorkspaceUserInput): RIO[DataSource, Long] =
    val now            = Instant.now()
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .filter(r => r.workspaceId == lift(data.workspaceId) && r.userId == lift(data.userId))
        .update(
          _.status    -> lift(data.status),
          _.`type`    -> lift(data.`type`),
          _.source    -> lift(data.source),
          _.inviterId -> lift(data.inviterId),
          _.updatedAt -> lift(now)
        )

    run(queries)

  override def delete(workspaceId: UUID, userId: UUID): RIO[DataSource, Long] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .filter(r => r.workspaceId == lift(workspaceId) && r.userId == lift(userId))
        .delete

    run(queries)

  override def deleteMany(userId: UUID): RIO[DataSource, Long] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .filter(_.userId == lift(userId))
        .delete

    run(queries)

  override def findUnique(id: UUID): RIO[DataSource, Option[DbWorkspaceUserPermissions]] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .filter(_.id == lift(id))

    run(queries).map(_.headOption)

  override def findActive(workspaceId: UUID, userId: UUID): RIO[DataSource, Option[DbWorkspaceUserPermissions]] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .filter(row =>
          row.workspaceId == lift(workspaceId) &&
            row.userId == lift(userId) &&
            row.status == lift(WorkspaceMemberStatus.Accepted.toString)
        )

    run(queries).map(_.headOption)

  override def findAdmin(workspaceId: UUID): RIO[DataSource, List[DbWorkspaceUserPermissions]] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .filter(row =>
          row.workspaceId == lift(workspaceId) &&
            row.`type` == lift(10) &&
            row.status == lift(WorkspaceMemberStatus.Accepted.toString)
        )

    run(queries)

  def tobeAllocate(workspaceId: UUID, count: Long): RIO[DataSource, List[DbWorkspaceUserPermissions]] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .filter(_.workspaceId == lift(workspaceId))
        .filter(p =>
          p.status == lift(WorkspaceMemberStatus.AllocatingSeat.toString) || p.status == lift(
            WorkspaceMemberStatus.NeedMoreSeat.toString
          )
        )
        .take(lift(count.toInt))

    run(queries)

  override def count(workspaceId: UUID): RIO[DataSource, Long] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .filter(_.workspaceId == lift(workspaceId))
        .size

    run(queries)

  override def countByStatus(workspaceId: UUID, status: String): RIO[DataSource, Long] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .filter(row => row.workspaceId == lift(workspaceId) && row.status == lift(status))
        .size

    run(queries)

  override def findByStatus(workspaceId: UUID, status: String): RIO[DataSource, List[DbWorkspaceUserPermissions]] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .filter(row =>
          row.workspaceId == lift(workspaceId) &&
            row.status == lift(status)
        )

    run(queries)

  override def findByUser(userId: UUID): RIO[DataSource, List[DbWorkspaceUserPermissions]] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .filter(row =>
          row.userId == lift(userId) &&
            row.status == lift(WorkspaceMemberStatus.Accepted.toString)
        )

    run(queries)

  override def findByUserRole(userId: UUID, role: Int): RIO[DataSource, List[DbWorkspaceUserPermissions]] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .filter(row =>
          row.userId == lift(userId) &&
            row.status == lift(WorkspaceMemberStatus.Accepted.toString) &&
            row.`type` == lift(role)
        )

    run(queries)

  override def paginate(
    workspaceId: UUID,
    first: Index,
    offset: Index
  ): ZIO[DataSource, Throwable, List[(DbWorkspaceUserPermissions, DbUsers)]] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .join(DbUsers.schema)
        .on(_.userId == _.id)
        .filter { case (perm, _) => perm.workspaceId == lift(workspaceId) }
        .map { case (perm, user) => (perm, user) }
        .sortBy { case (perm, _) => perm.createdAt }(Ord.asc)
        .take(lift(first))
        .drop(lift(offset))

    run(queries)

  override def paginateAfter(
    workspaceId: UUID,
    after: Instant,
    first: Int,
    offset: Int
  ): RIO[DataSource, List[(DbWorkspaceUserPermissions, DbUsers)]] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .join(DbUsers.schema)
        .on(_.userId == _.id)
        .filter { case (perm, _) => perm.workspaceId == lift(workspaceId) }
        .filter { case (perm, _) => perm.createdAt >= lift(after) }
        .map { case (perm, user) => (perm, user) }
        .sortBy { case (perm, _) => perm.createdAt }(Ord.asc)
        .take(lift(first))
        .drop(lift(offset + 1))

    run(queries)

  override def search(
    workspaceId: UUID,
    query: String,
    after: Option[Instant],
    first: Int,
    offset: Int
  ): RIO[DataSource, List[(DbWorkspaceUserPermissions, DbUsers)]] =
    inline def queries = quote:
      DbWorkspaceUserPermissions.schema
        .join(DbUsers.schema)
        .on(_.userId == _.id)
        .filter { case (perm, _) => perm.workspaceId == lift(workspaceId) }
        .filter { case (perm, _) => perm.status == lift(WorkspaceMemberStatus.Accepted.toString) }
        .filter { case (_, user) =>
          sql"LOWER(${user.email}) LIKE ${lift(s"%${query.toLowerCase}%")} OR LOWER(${user.name}) LIKE ${lift(s"%${query.toLowerCase}%")}"
            .as[Boolean]
        }
        .map { case (perm, user) => (perm, user) }
        .sortBy { case (perm, _) => perm.createdAt }(Ord.asc)
        .take(lift(first))
        .drop(lift(offset + (if (after.isDefined) 1 else 0)))

    run(queries)

object WorkspaceUserDaoImpl:
  val live: URLayer[DataSource, WorkspaceUserDao] = ZLayer.fromFunction(WorkspaceUserDaoImpl.apply _)
