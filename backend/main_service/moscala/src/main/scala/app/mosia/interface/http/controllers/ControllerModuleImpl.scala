package app.mosia.interface.http.controllers

import app.mosia.core.configs.AppConfig
import app.mosia.infra.cache.CacheProvider
import app.mosia.infra.eventbus.EventBus
import app.mosia.infra.helpers.crypto.CryptoHelper
import app.mosia.interface.http.controllers.auth.{AuthController, AuthControllerImpl}
import app.mosia.interface.http.controllers.health.{HealthController, HealthControllerImpl}
import app.mosia.interface.http.controllers.oauth.{OAuthController, OAuthControllerImpl}
import zio.*

import javax.sql.DataSource

case class ControllerModuleImpl(
  authController: AuthController,
  oauthController: OAuthController,
  healthController: HealthController
) extends ControllerModule

object ControllerModuleImpl:
  val layer: ZLayer[DataSource & Ref[AppConfig] & EventBus, Throwable, ControllerModuleImpl] =
    (AuthControllerImpl.live ++ OAuthControllerImpl.live ++ HealthControllerImpl.live).map { env =>
      ZEnvironment(
        ControllerModuleImpl(
          authController = env.get[AuthController],
          oauthController = env.get[OAuthController],
          healthController = env.get[HealthController]
        )
      )
    }
