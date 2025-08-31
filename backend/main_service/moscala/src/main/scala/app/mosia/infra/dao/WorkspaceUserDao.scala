package app.mosia.infra.dao

import app.mosia.domain.model.*
import app.mosia.domain.model.input.WorkspaceUserInput
import app.mosia.models.{ DbUsers, DbWorkspaceUserPermissions }
import zio.{ RIO, ZIO }

import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

trait WorkspaceUserDao {
  def findOwner(workspaceId: UUID): RIO[DataSource, Option[DbWorkspaceUserPermissions]]
  def findAdmin(workspaceId: UUID): RIO[DataSource, List[DbWorkspaceUserPermissions]]
  def findUser(workspaceId: UUID, userId: UUID): RIO[DataSource, Option[DbWorkspaceUserPermissions]]
  def findUnique(id: UUID): RIO[DataSource, Option[DbWorkspaceUserPermissions]]
  def tobeAllocate(workspaceId: UUID, count: Long): RIO[DataSource, List[DbWorkspaceUserPermissions]]
  def findByUser(userId: UUID): RIO[DataSource, List[DbWorkspaceUserPermissions]]
  def findByUserRole(userId: UUID, role: Int): RIO[DataSource, List[DbWorkspaceUserPermissions]]
  def findActive(workspaceId: UUID, userId: UUID): RIO[DataSource, Option[DbWorkspaceUserPermissions]]
  def findByStatus(workspaceId: UUID, status: String): RIO[DataSource, List[DbWorkspaceUserPermissions]]
  def updateRole(id: UUID, role: Int): RIO[DataSource, Long]
  def updateStatus(id: UUID, status: String): RIO[DataSource, Long]
  def create(data: WorkspaceUserInput): RIO[DataSource, UUID]
  def update(data: WorkspaceUserInput): RIO[DataSource, Long]
  def delete(workspaceId: UUID, userId: UUID): RIO[DataSource, Long]
  def deleteMany(userId: UUID): RIO[DataSource, Long]
  def count(workspaceId: UUID): RIO[DataSource, Long]
  def countByStatus(workspaceId: UUID, status: String): RIO[DataSource, Long]
  def paginate(workspaceId: UUID, first: Int, offset: Int): RIO[DataSource, List[(DbWorkspaceUserPermissions, DbUsers)]]
  def paginateAfter(
    workspaceId: UUID,
    after: Instant,
    first: Int,
    offset: Int
  ): RIO[DataSource, List[(DbWorkspaceUserPermissions, DbUsers)]]
  def search(
    workspaceId: UUID,
    query: String,
    after: Option[Instant],
    first: Int,
    offset: Int
  ): RIO[DataSource, List[(DbWorkspaceUserPermissions, DbUsers)]]
}
