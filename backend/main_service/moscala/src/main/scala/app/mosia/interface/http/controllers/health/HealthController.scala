package app.mosia.interface.http.controllers.health

import zio.Task

trait HealthController:
  def healthCheck: Task[String]
