package app.mosia.core.configs

import zio.json.JsonCodec

final case class CryptoConfig(privateKey: Option[String], jwtKey: Option[String]) derives JsonCodec
