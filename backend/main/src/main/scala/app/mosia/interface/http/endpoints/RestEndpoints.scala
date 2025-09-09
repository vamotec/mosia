package app.mosia.interface.http.endpoints

import app.mosia.interface.http.api.*
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.ServerEndpoint
import zio.Task

object RestEndpoints:
  def make(
    authApi: AuthApi,
    oAuthApi: OAuthApi,
    healthApi: HealthApi
  ): List[ServerEndpoint[ZioStreams, Task]] = authApi.endpoints ++ oAuthApi.endpoints ++ healthApi.endpoints
