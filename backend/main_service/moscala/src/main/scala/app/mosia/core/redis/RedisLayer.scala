package app.mosia.core.redis

import app.mosia.core.configs.AppConfig
import zio.*
import zio.redis.*

object RedisLayer:
  /** 单节点 Redis，通过 AppConfig 或环境变量读取 host/port */
  private def buildRedisLayer(config: AppConfig): ZLayer[Any, Throwable, Redis] =
    (ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier) ++
      ZLayer.succeed(RedisConfig(host = config.redis.host, port = config.redis.port))
      ) >>> Redis.singleNode

  val singleNode: ZLayer[Ref[AppConfig], Throwable, Redis] =
    ZLayer.fromZIO(
      ZIO.serviceWithZIO[Ref[AppConfig]](_.get.map(buildRedisLayer))
    ).flatten
