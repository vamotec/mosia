package app.mosia.infra.workspace

import java.util.UUID

case class WorkspaceDataType(
  status: Option[WorkspaceMemberStatus],
  source: Option[WorkspaceMemberSource],
  inviterId: Option[UUID]
)
