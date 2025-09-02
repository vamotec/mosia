package app.mosia.infra.cache

import app.mosia.core.configs.AppConfig
import app.mosia.core.redis.{RedisLayer, RedisPrefix}
import zio.*
import zio.redis.Redis

private case class CacheProviderImpl(
  session: CacheNamespace,
  cache: CacheNamespace,
  event: CacheNamespace
) extends CacheProvider

object CacheProviderImpl:
  val live: ZLayer[Redis, Nothing, CacheProvider] =
    ZLayer.fromZIO:
      for redis <- ZIO.service[Redis]
      yield new CacheProviderImpl(
        session = CacheNamespaceImpl(RedisPrefix("session:"), redis),
        cache = CacheNamespaceImpl(RedisPrefix("cache:"), redis),
        event = CacheNamespaceImpl(RedisPrefix("event:"), redis)
      )

  val cacheCombine: ZLayer[Ref[AppConfig], Throwable, CacheProvider] = RedisLayer.singleNode >>> CacheProviderImpl.live
