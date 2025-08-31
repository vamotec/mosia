package app.mosia.domain.model.input

import java.util.UUID

case class UserFeaturesInput(
  userId: UUID,
  featureId: Int,
  reason: String,
  activated: Boolean,
  name: String,
  expiredAt: Option[java.time.Instant],
  `type`: Int
)
