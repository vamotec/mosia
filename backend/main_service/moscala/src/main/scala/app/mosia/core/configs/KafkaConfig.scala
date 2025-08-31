package app.mosia.core.configs

import zio.json.*

final case class KafkaConfig(
  bootstrapServers: List[String],
  groupId: String,
  autoCommit: Boolean,
  offsetStrategy: String
) derives JsonCodec
