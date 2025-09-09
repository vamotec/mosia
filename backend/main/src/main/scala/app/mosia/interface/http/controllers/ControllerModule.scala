package app.mosia.interface.http.controllers

import app.mosia.interface.http.controllers.auth.AuthController
import app.mosia.interface.http.controllers.health.HealthController
import app.mosia.interface.http.controllers.oauth.OAuthController

trait ControllerModule:
  def authController: AuthController
  def oauthController: OAuthController
  def healthController: HealthController
