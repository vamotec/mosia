package app.mosia.interface.http.endpoints

import app.mosia.interface.http.api.HealthApi
import app.mosia.interface.http.controllers.ControllerModule
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.endpoint
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ZLayer}

case class HealthEndpoint (controller: ControllerModule) extends HealthApi:
  override def endpoints: List[ServerEndpoint[ZioStreams, Task]] = List(healthCheckEndpoint)
  
  private val healthCheckEndpoint: ServerEndpoint[ZioStreams, Task] =
    endpoint.get
      .in("api" / "health")
      .out(jsonBody[String])
      .zServerLogic { _ =>
        controller.healthController
          .healthCheck
          .mapBoth(e => s"Health check failed: $e", response => response)
      }

object HealthEndpoint:
  val live: ZLayer[ControllerModule, Nothing, HealthEndpoint] = ZLayer.fromFunction(new HealthEndpoint(_))