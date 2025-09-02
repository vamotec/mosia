package app.mosia.interface.http.controllers.health

import zio.*

case class HealthControllerImpl () extends HealthController:
  override def healthCheck: Task[String] = ZIO.succeed("OK")

object HealthControllerImpl:
  val live: ULayer[HealthController] = ZLayer.succeed(new HealthControllerImpl())