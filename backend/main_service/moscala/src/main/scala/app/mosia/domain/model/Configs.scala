package app.mosia.domain.model

import zio.json.JsonCodec

import java.util.UUID

case class Configs(
  id: String,
  config: ConfigsPayload,
  updatedAt: java.time.Instant,
  lastUpdatedBy: Option[UUID]
) derives JsonCodec
