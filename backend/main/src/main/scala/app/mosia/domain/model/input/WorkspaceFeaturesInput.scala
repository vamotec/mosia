package app.mosia.domain.model.input

import app.mosia.core.types.JSONValue

import java.time.Instant
import java.util.UUID

case class WorkspaceFeaturesInput(
  id: Int,
  workspaceId: UUID,
  featureId: Int,
  reason: String,
  expiredAt: Option[Instant],
  activated: Boolean,
  configs: JSONValue,
  name: String,
  `type`: Int
)
