package app.mosia.domain.model

import app.mosia.domain.model.Id.WorkspaceUserId

import java.util.UUID

case class WorkspaceUser(
  id: WorkspaceUserId,
  workspaceId: UUID,
  userId: UUID,
  `type`: Int,
  accepted: Boolean,
  status: String,
  inviterId: Option[UUID],
  source: String
)
