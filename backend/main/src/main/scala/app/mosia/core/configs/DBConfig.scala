package app.mosia.core.configs

import zio.json.JsonCodec

case class DBConfig(
  jdbcUrl: String,
  username: String,
  password: String,
  maximumPoolSize: Int
) derives JsonCodec
