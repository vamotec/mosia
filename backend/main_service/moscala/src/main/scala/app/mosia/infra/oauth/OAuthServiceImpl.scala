package app.mosia.infra.oauth

import app.mosia.core.configs.AppConfig
import app.mosia.infra.oauth.Types.*
import app.mosia.infra.cache.{ CacheProvider, CacheProviderImpl, CacheSetOptions }
import app.mosia.infra.eventbus.EventBus
import app.mosia.infra.helpers.crypto.CryptoHelper
import zio.redis.RedisError
import zio.{ durationInt, IO, Ref, Task, ZIO, ZLayer }

import java.util.UUID
import javax.sql.DataSource

case class OAuthServiceImpl(cache: CacheProvider, factory: OAuthProviderFactory) extends OAuthService {
  override def isValidState(stateStr: String): Task[Boolean] =
    ZIO.succeed(stateStr.length == 36)

  override def saveOAuthState(state: OAuthState): IO[RedisError, String] =
    for {
      token <- ZIO.succeed(UUID.randomUUID().toString)
      _     <- cache.cache.set(s"oauth_state:$token", state, CacheSetOptions(ttl = Some(3.hours)))
    } yield token

  override def getOAuthState(token: String): Task[Option[OAuthState]] =
    cache.cache.get[OAuthState](s"oauth_state:$token")

  override def availableOAuthProviders(name: String): Task[OAuthProviderName] = {
    val providerName = OAuthProviderName.valueOf(name)
    factory.get(providerName).flatMap {
      case Some(provider) => ZIO.succeed(provider.provider)
      case None           => ZIO.fail(new IllegalArgumentException(s"OAuth provider not found: $name"))
    }
  }
}

object OAuthServiceImpl:
  def make: ZIO[OAuthProviderFactory & CacheProvider, Nothing, OAuthService] =
    for
      cache   <- ZIO.service[CacheProvider]
      factory <- ZIO.service[OAuthProviderFactory]
    yield new OAuthServiceImpl(cache, factory)

  val live: ZLayer[DataSource & Ref[AppConfig] & EventBus, Throwable, OAuthService] =
    OAuthProviderFactoryImpl.live ++ CacheProviderImpl.cacheCombine >>> ZLayer.fromZIO(make)
