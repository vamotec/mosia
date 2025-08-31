package app.mosia.core.configs

import app.mosia.core.configs.Predefined.*
import app.mosia.core.configs.Predefined.MOSIA_ENV.Dev
import zio.config.magnolia.deriveConfig
import zio.{ BuildInfo, Config, Ref, ZIO, ZLayer }
import zio.json.{ DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder }
import zio.redis.RedisConfig

case class AppConfig(
  app: AppSettings,
  mosiaEnv: MOSIA_ENV,
  scala: ScalaConfig,
  auth: AuthConfig,
  database: DBConfig,
  server: ServerConfig,
  crypto: CryptoConfig,
  kafka: KafkaConfig,
  mailer: MailerConfig,
  flags: FlagsConfig
) derives JsonCodec

object AppConfig:
  private val descriptor: Config[AppConfig] =
    deriveConfig[AppConfig]
      .nested("app")

  val live: ZLayer[Any, Config.Error, Ref[AppConfig]] = ZLayer.fromZIO(
    for
      config    <- ZIO.config(descriptor)
      configRef <- Ref.make(config)
      _         <- ZIO.logInfo(s"Loaded AppConfig: $config")
      _          = Env.dev = config.mosiaEnv == Dev
      _          = Env.version = BuildInfo.version
    yield configRef
  )

  given JsonEncoder[RedisConfig] = DeriveJsonEncoder.gen[RedisConfig]
  given JsonDecoder[RedisConfig] = DeriveJsonDecoder.gen[RedisConfig]
