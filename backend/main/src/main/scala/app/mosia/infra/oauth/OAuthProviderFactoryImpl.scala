package app.mosia.infra.oauth

import app.mosia.core.configs.AppConfig
import app.mosia.core.errors.UserFriendlyError
import app.mosia.infra.eventbus.EventBus
import app.mosia.infra.oauth.Types.*
import app.mosia.infra.features.ServerFeature
import app.mosia.infra.helpers.server.{ ServerHelper, ServerHelperImpl }
import app.mosia.infra.oauth.providers.OAuthProvider
import zio.*

import javax.sql.DataSource

final case class OAuthProviderFactoryImpl(
  server: ServerHelper,
  providersMapRef: Ref[Map[OAuthProviderName, OAuthProvider]]
) extends OAuthProviderFactory:
  // 返回提供者列表
  override def provider: Task[List[OAuthProviderName]] =
    providersMapRef.get.map(_.keys.toList)

  // 获取提供者
  override def get(name: OAuthProviderName): Task[Option[OAuthProvider]] =
    providersMapRef.get.map(_.get(name))

  // 注册提供者
  override def register(name: OAuthProviderName, provider: OAuthProvider): Task[Unit] = {
    providersMapRef.update(_ + (name -> provider))
    ZIO.logInfo(s"OAuth provider [${provider.provider}] registered.")
  } *> server.enableFeature(ServerFeature.OAuth)

  // 移除提供者
  override def unregister(provider: OAuthProvider): Task[Unit] = {
    val providerName = provider.provider
    providersMapRef.update(_ - providerName)
    ZIO.logInfo(s"OAuth provider [${provider.provider}] unregistered.")
  } *>
    providersMapRef.get.flatMap { map =>
      if (map.isEmpty)
        server.disableFeature(ServerFeature.OAuth)
      else
        ZIO.unit
    }

object OAuthProviderFactoryImpl:
  // 创建工厂实例的方法
  def make: ZIO[ServerHelper, Nothing, OAuthProviderFactory] =
    for
      server <- ZIO.service[ServerHelper]
      ref    <- Ref.make(Map.empty[OAuthProviderName, OAuthProvider])
    yield new OAuthProviderFactoryImpl(server, ref)

  val live: ZLayer[DataSource & Ref[AppConfig] & EventBus, Throwable, OAuthProviderFactory] =
    ServerHelperImpl.live >>> ZLayer.fromZIO(make)
