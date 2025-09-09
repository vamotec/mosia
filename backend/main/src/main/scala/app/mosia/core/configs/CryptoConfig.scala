package app.mosia.core.configs

import zio.json.JsonCodec

final case class CryptoConfig(privateKey: String, jwtKey: String) derives JsonCodec
