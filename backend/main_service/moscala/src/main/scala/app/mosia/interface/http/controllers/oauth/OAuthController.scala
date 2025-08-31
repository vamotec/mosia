package app.mosia.interface.http.controllers.oauth

import app.mosia.application.dto.JwtDto
import sttp.model.headers.CookieWithMeta
import sttp.tapir.model.ServerRequest
import zio.{ IO, RIO, Task }

import javax.sql.DataSource

trait OAuthController:
  def preflight(
    unknownProviderName: Option[String] = None,
    redirectUrl: Option[String] = None,
    client: Option[String] = None,
    clientNonce: Option[String] = None
  ): Task[String]

  def callback(
    request: ServerRequest,
    code: Option[String] = None,
    stateStr: Option[String] = None,
    clientNonce: Option[String] = None
  ): Task[(JwtDto, List[CookieWithMeta])]
