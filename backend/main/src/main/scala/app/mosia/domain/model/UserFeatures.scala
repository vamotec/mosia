package app.mosia.domain.model

import zio.json.*

import java.util.UUID

case class UserFeatures(
  id: Int,
  userId: UUID,
  featureId: Int,
  reason: String,
  createdAt: java.time.Instant,
  expiredAt: Option[java.time.Instant],
  activated: Boolean,
  name: String,
  `type`: Int
) derives JsonCodec
