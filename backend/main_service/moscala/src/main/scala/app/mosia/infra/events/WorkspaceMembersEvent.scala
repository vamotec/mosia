package app.mosia.infra.events

import app.mosia.infra.workspace.Permission
import zio.json.*

import java.util.UUID

sealed trait WorkspaceMembersEvent

object WorkspaceMembersEvent:
  final case class Invite(inviterId: UUID, inviteId: UUID) extends WorkspaceMembersEvent

  final case class Removed(workspaceId: UUID, userId: UUID) extends WorkspaceMembersEvent

  final case class Leave(workspaceId: UUID, userId: UUID) extends WorkspaceMembersEvent

  final case class Update(workspaceId: UUID) extends WorkspaceMembersEvent

  final case class AllocateSeats(workspaceId: UUID, quantity: Int) extends WorkspaceMembersEvent

  final case class RoleChanged(workspaceId: UUID, userId: UUID, role: Option[Permission])

  given JsonEncoder[RoleChanged] = DeriveJsonEncoder.gen[RoleChanged]

  given JsonDecoder[RoleChanged] = DeriveJsonDecoder.gen[RoleChanged]
