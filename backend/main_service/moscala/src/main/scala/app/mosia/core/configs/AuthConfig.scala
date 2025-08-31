package app.mosia.core.configs

import zio.config.typesafe.*
import app.mosia.core.configs.Predefined.*
import zio.json.JsonCodec

final case class AuthConfig(
  session: SessionConfig,
  allowSignup: Boolean,
  requireEmailDomainVerification: Boolean,
  requireEmailVerification: Boolean,
  password: PasswordPolicy
) derives JsonCodec
