package app.mosia.interface.http.endpoints

import app.mosia.application.dto.JwtDto
import app.mosia.core.errors.ErrorResponse
import app.mosia.interface.http.api.OAuthApi
import app.mosia.interface.http.controllers.ControllerModule
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.ztapir.*
import sttp.tapir.{ endpoint, extractFromRequest }
import zio.{ Task, ZIO, ZLayer }

case class OAuthEndpoint(controller: ControllerModule) extends OAuthApi:
  override def endpoints: List[ServerEndpoint[ZioStreams, Task]] =
    List(preflightEndpoint, callbackEndpoint)

  private val preflightEndpoint: ServerEndpoint[ZioStreams, Task] =
    endpoint.post
      .in("api" / "oauth" / "preflight")
      .in(query[Option[String]]("unknownProviderName"))
      .in(query[Option[String]]("redirectUri"))
      .in(query[Option[String]]("client"))
      .in(query[Option[String]]("clientNonce"))
      .out(jsonBody[String])
      .zServerLogic { case (unknownProviderName, redirectUri, client, clientNonce) =>
        controller.oauthController
          .preflight(unknownProviderName, redirectUri, client, clientNonce)
          .mapError(err => ErrorResponse("AuthenticationFailed", err.getMessage))
      }

  private val callbackEndpoint: ServerEndpoint[ZioStreams, Task] =
    endpoint.post
      .in(extractFromRequest[ServerRequest](identity))
      .in(query[Option[String]]("code"))
      .in(query[Option[String]]("stateStr"))
      .in(query[Option[String]]("clientNonce"))
      .out(jsonBody[JwtDto].and(setCookies))
      .zServerLogic { case (request, code, stateSrt, clientNonce) =>
        controller.oauthController
          .callback(request, code, stateSrt, clientNonce)
          .mapError(err => ErrorResponse("AuthenticationFailed", err.getMessage))
      }

object OAuthEndpoint:
  val live: ZLayer[ControllerModule, Nothing, OAuthEndpoint] =
    ZLayer.fromFunction(new OAuthEndpoint(_))

end OAuthEndpoint
