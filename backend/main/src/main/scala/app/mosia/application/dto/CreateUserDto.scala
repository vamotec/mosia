package app.mosia.application.dto

import app.mosia.core.errors.UserFriendlyError.ValidationError
import app.mosia.domain.model.Email
import app.mosia.domain.model.Email.Email
import sttp.tapir.Schema
import zio.json.*
import zio.*

import scala.util.Try

case class CreateUserDto(
  email: String,
  password: Option[String],
  name: Option[String] = None
) derives JsonCodec,
      Schema:
  def toEmail: Task[Email] =
    ZIO.fromTry(Try(Email(email))).mapError(_ => ValidationError(info = "Invalid email format"))
