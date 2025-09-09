package app.mosia.core.configs

import zio.json.JsonCodec

case class FlagsConfig(earlyAccessControl: Boolean) derives JsonCodec
