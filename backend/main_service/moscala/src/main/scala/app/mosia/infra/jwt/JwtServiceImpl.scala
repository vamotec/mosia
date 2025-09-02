package app.mosia.infra.jwt

import app.mosia.application.dto.{JwtPayload, UserResponseDto}
import app.mosia.core.configs.AppConfig
import app.mosia.core.errors.*
import app.mosia.domain.model.Users
import app.mosia.infra.helpers.crypto.CryptoUtils
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import zio.*
import zio.json.*

import scala.util.{Failure, Success, Try}

case class JwtServiceImpl(secret: String) extends JwtService:
  def generateToken(user: Users): Task[String] = ZIO.attempt:
    val payload = JwtPayload.fromUser(user)
    val claim   = JwtClaim(payload.toJson)
      .issuedAt(payload.iat)
      .expiresAt(payload.exp)
    Jwt.encode(claim, secret, JwtAlgorithm.HS256)

  def validateToken(token: String): IO[JwtError, JwtPayload] =
    ZIO
      .fromTry(Jwt.decodeRaw(token, secret, Seq(JwtAlgorithm.HS256)))
      .mapError:
        case _: java.security.SignatureException     => InvalidTokenError("Invalid signature")
        case ex if ex.getMessage.contains("expired") => ExpiredTokenError("Token expired")
        case ex                                      => MalformedTokenError(s"Malformed token: ${ex.getMessage}")
      .flatMap: json =>
        ZIO
          .fromEither(json.fromJson[JwtPayload])
          .mapError(error => MalformedTokenError(s"Failed to decode payload: $error"))

object JwtServiceImpl:
  val live: ZLayer[Ref[AppConfig], Nothing, JwtService] = ZLayer.fromZIO:
    for
      ref <- ZIO.service[Ref[AppConfig]]
      cfg <- ref.get
      jwtKey <- ZIO
        .fromOption(cfg.crypto.jwtKey)
        .orElse(CryptoUtils.generatePrivateKey())
        .tapError(e => ZIO.logError(s"Failed to get or generate jwt key: ${e.getMessage}"))
    yield new JwtServiceImpl(jwtKey)

