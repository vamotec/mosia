package app.mosia.interface.http.api

import app.mosia.interface.http.controllers.ControllerModule
import app.mosia.interface.http.endpoints.*
import zio.*

final case class ApiModule(
  authApi: AuthApi,
  oAuthApi: OAuthApi,
  healthApi: HealthApi
)

object ApiModule:
  val live: URLayer[ControllerModule, ApiModule] =
    ZLayer.fromFunction: (controller: ControllerModule) =>
      ApiModule(
        new AuthEndpoint(controller),
        new OAuthEndpoint(controller),
        new HealthEndpoint(controller)
      )
