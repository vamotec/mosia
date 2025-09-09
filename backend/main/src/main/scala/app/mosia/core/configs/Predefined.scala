package app.mosia.core.configs

import zio.json.JsonCodec

object Predefined:
  enum MOSIA_ENV derives JsonCodec:
    case Dev, Beta, Prod

  case class FlavorConfig(
    `type`: String,
    allinone: Boolean,
    graphql: Boolean,
    sync: Boolean,
    renderer: Boolean,
    doc: Boolean,
    script: Boolean
  ) derives JsonCodec

  enum ScalaConfig derives JsonCodec:
    case Prod, Dev, Test

  enum MosiaConfig derives JsonCodec:
    case Canary, Beta, Stable

  case class SessionConfig(ttl: Int, ttr: Int) derives JsonCodec

  case class PasswordPolicy(min: Int, max: Int) derives JsonCodec
