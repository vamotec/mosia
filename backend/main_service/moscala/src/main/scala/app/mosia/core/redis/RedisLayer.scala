package app.mosia.core.redis

import zio.*
import zio.redis.*

object RedisLayer {

  /** 单节点 Redis */
  val singleNode: ZLayer[Any, Throwable, Redis] =
    (
      ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier) ++
        ZLayer.succeed(RedisConfig.Local)
    ) >>> Redis.singleNode

  /** Redis Cluster 模式 */
  val cluster: ZLayer[Any, Throwable, Redis] = {
    val clusterConfig = RedisClusterConfig(
      addresses = Chunk(
        RedisUri("localhost", 7000),
        RedisUri("localhost", 7001),
        RedisUri("localhost", 7002)
      ),
      retry = RetryClusterConfig(base = 100.millis, factor = 1.5, maxRecurs = 5)
    )

    (
      ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier) ++
        ZLayer.succeed(clusterConfig)
    ) >>> Redis.cluster
  }
}
