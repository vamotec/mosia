package app.mosia.core.configs

import zio.json.{ DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder }

case class ServerConfig(
  externalUrl: Option[String],
  https: Boolean,
  host: String,
  port: Int,
  path: String,
  name: Option[String]
) derives JsonCodec
