package app.mosia.interface.http.endpoints

import app.mosia.application.dto.*
import app.mosia.core.errors.ErrorResponse
import app.mosia.domain.model.CurrentUser
import app.mosia.infra.auth.Types.sessionCookieName
import app.mosia.interface.http.api.AuthApi
import app.mosia.interface.http.controllers.ControllerModule
import sttp.capabilities.zio.ZioStreams
import sttp.model.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.ztapir.*
import zio.json.DecoderOps
import zio.{ Task, ZIO, ZLayer }

case class AuthEndpoint(controller: ControllerModule) extends AuthApi:
  override def endpoints: List[ServerEndpoint[ZioStreams, Task]] =
    List(checkEmailEndpoint, signInEndpoint, signUpEndpoint, signOutEndpoint, sessionsEndpoint, magicLinkEndpoint)

  private val checkEmailEndpoint: ServerEndpoint[ZioStreams, Task] =
    endpoint.post
      .in("api" / "auth" / "check-email")
      .in(jsonBody[String])
      .out(jsonBody[EmailCheckResponseDto])
      .errorOut(statusCode(StatusCode.Unauthorized).and(stringBody))
      .zServerLogic { email =>
        controller.authController
          .checkEmailStatus(Some(email))
          .mapBoth(e => s"Authentication failed: $e", response => response)
      }

  private val signInEndpoint: ServerEndpoint[ZioStreams, Task] =
    endpoint.post
      .in("api" / "auth" / "sign-in")
      .in(extractFromRequest[ServerRequest](identity))
      .in(jsonBody[OptionLoginDto])
      .in(query[Option[String]]("redirectUri"))
      .out(jsonBody[JwtDto].and(setCookies))
      .errorOut(
        jsonBody[ErrorResponse]
          .description("Auth failed")
          .example(ErrorResponse("InvalidPassword", "PasswordIncorrect"))
      )
      .zServerLogic { case (request, credential, redirectUri) =>
        controller.authController
          .signIn(request, credential, redirectUri)
          .mapError(err => ErrorResponse("SignInFailed", err.getMessage))
      }

  private val signUpEndpoint: ServerEndpoint[ZioStreams, Task] =
    endpoint.post
      .in("api" / "auth" / "sign-up")
      .in(extractFromRequest[ServerRequest](identity))
      .in(jsonBody[CreateUserDto])
      .out(jsonBody[JwtDto].and(setCookies))
      .errorOut(
        jsonBody[ErrorResponse]
          .description("Register failed")
          .example(ErrorResponse("EmailAlreadyExit", "al"))
      )
      .zServerLogic { case (request, credential) =>
        controller.authController
          .signUp(request, credential)
          .mapError(err => ErrorResponse("SignUpFailed", err.getMessage))
      }

  private val signOutEndpoint: ServerEndpoint[ZioStreams, Task] =
    endpoint.post
      .in("api" / "auth" / "sign-out")
      .in(extractFromRequest[ServerRequest](identity))
      .out(statusCode(StatusCode.Ok))
      .out(jsonBody[MessageResponseDto].and(setCookies))
      .zServerLogic { request =>
        {
          for {
            bodyStr      <- controller.authController.currentSessionUsers(request)
            sessionId     =
              request.cookies.collectFirst { case Right(c) if c.name == sessionCookieName => c.value }
            json         <- ZIO.fromEither(bodyStr.fromJson[Map[String, List[String]]])
            firstOpt      = json.get("users").flatMap(_.headOption)
            firstUserOpt <- firstOpt match
                              case Some(jsonStr) => ZIO.fromEither(jsonStr.fromJson[CurrentUser]).map(Some(_))
                              case None          => ZIO.succeed(None)
            userId        = firstUserOpt.map(_.id.value.toString)
            result       <-
              controller.authController
                .signOut(responseData = MessageResponseDto("sign-out"), sessionId = sessionId, userId = userId)
          } yield result
        }.mapError(_ => StatusCode.InternalServerError)
      }

  private val sessionsEndpoint: ServerEndpoint[ZioStreams, Task] =
    endpoint.get
      .in("api" / "auth" / "sessions")
      .in(extractFromRequest[ServerRequest](identity))
      .out(jsonBody[String])
      .zServerLogic { request =>
        controller.authController
          .currentSessionUsers(request)
          .mapError(err => ErrorResponse("getSessionsFailed", err.getMessage))
      }

  private val magicLinkEndpoint: ServerEndpoint[ZioStreams, Task] =
    endpoint.post
      .in("api" / "auth" / "magic-link")
      .in(extractFromRequest[ServerRequest](identity))
      .in(jsonBody[MagicLinkDto])
      .out(statusCode(StatusCode.Ok))
      .out(jsonBody[JwtDto].and(setCookies))
      .zServerLogic { case (request, credential) =>
        controller.authController
          .magicLinkSignIn(request, credential)
          .mapError(err => ErrorResponse("magicLinkFailed", err.getMessage))
      }

//  private val healthCheckEndpoint: ServerEndpoint[ZioStreams, Task] =
//    endpoint.get
//      .in("api" / "health")
//      .out(jsonBody[String])
//      .zServerLogic { _ =>
//        controller.authController
//          .healthCheck()
//          .mapBoth(e => s"Health check failed: $e", response => response)
//      }

object AuthEndpoint:
  val live: ZLayer[ControllerModule, Nothing, AuthEndpoint] =
    ZLayer.fromFunction(new AuthEndpoint(_))

end AuthEndpoint
