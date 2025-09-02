package app.mosia.core.configs

import zio.json.JsonCodec

case class RedisConf (
  host: String,
  port: Int,
  clusterHosts: Option[List[String]]
) derives JsonCodec
