package app.mosia.infra.jwt

import app.mosia.application.dto.*
import app.mosia.core.errors.JwtError
import app.mosia.domain.model.Users
import zio.*

trait JwtService:
  def generateToken(user: Users): Task[String]
  def validateToken(token: String): IO[JwtError, JwtPayload]
