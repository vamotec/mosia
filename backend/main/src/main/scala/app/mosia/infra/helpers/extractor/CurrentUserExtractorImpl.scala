package app.mosia.infra.helpers.extractor

import app.mosia.domain.model.CurrentUser
import app.mosia.infra.jwt.JwtService
import zio.http.{ Header, Request }
import zio.*

case class CurrentUserExtractorImpl(jwt: JwtService) extends CurrentUserExtractor:
  override def getCurrent(request: Request): UIO[CurrentUser] =
    request.header(Header.Authorization.Bearer) match
      case Some(bearer) =>
        val raw = bearer.token.value.toArray.mkString
        (
          for
            _       <- ZIO.logDebug(s"🔐 Got token: $raw")
            payload <- jwt.validateToken(raw)
            user    <- CurrentUser.fromJwtPayload(payload)
          yield user
        ).catchAll { err =>
          ZIO.logWarning(s"⚠️ Failed to parse token: $err") *> ZIO.succeed(CurrentUser.guest)
        }
      case None         => ZIO.succeed(CurrentUser.guest)

object CurrentUserExtractorImpl:
  val live: ZLayer[JwtService, Nothing, CurrentUserExtractorImpl] =
    ZLayer.fromFunction(CurrentUserExtractorImpl.apply)
