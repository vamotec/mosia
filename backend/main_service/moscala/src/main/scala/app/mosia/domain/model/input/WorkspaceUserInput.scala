package app.mosia.domain.model.input

import java.util.UUID

case class WorkspaceUserInput(
  workspaceId: UUID,
  userId: UUID,
  `type`: Int,
  status: String,
  inviterId: Option[UUID],
  source: String
)
