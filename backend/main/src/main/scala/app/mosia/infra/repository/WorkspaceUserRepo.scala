package app.mosia.infra.repository

import app.mosia.domain.model.*
import app.mosia.infra.workspace.*
import zio.{ RIO, Task, ZIO }

import java.util.UUID
import javax.sql.DataSource

trait WorkspaceUserRepo:
  def setOwner(workspaceId: UUID, userId: UUID): RIO[DataSource, Unit]
  def set(workspaceId: UUID, userId: UUID, role: Permission, defaultData: WorkspaceDataType): RIO[DataSource, Unit]
  def setStatus(workspaceId: UUID, userId: UUID, defaultData: WorkspaceDataType): Task[Unit]
  def delete(workspaceId: UUID, userId: UUID): Task[Long]
  def deleteByUserId(userId: UUID): Task[Long]
  def get(workspaceId: UUID, userId: UUID): Task[Option[WorkspaceUser]]
  def getById(id: UUID): Task[Option[WorkspaceUser]]
  def getActive(workspaceId: UUID, userId: UUID): Task[Option[WorkspaceUser]]
  def getOwner(workspaceId: UUID): Task[Option[Users]]
  def getAdmins(workspaceId: UUID): Task[List[Users]]
  def count(workspaceId: UUID): Task[Long]
  def statusCount(workspaceId: UUID, status: WorkspaceMemberStatus): Task[Long]
  def insufficientSeatMemberCount(workspaceId: UUID): Task[Long]
  def getUserActiveRoles(userId: UUID, role: Option[Permission]): Task[List[WorkspaceUser]]
  def paginate(workspaceId: UUID, pagination: PaginationInput): Task[List[(WorkspaceUser, Users)]]
  def search(workspaceId: UUID, query: String, pagination: PaginationInput): Task[List[(WorkspaceUser, Users)]]
  def allocateSeats(workspaceId: UUID, limit: Long): RIO[DataSource, List[WorkspaceUser]]
