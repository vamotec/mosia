package app.mosia.application.dto

import app.mosia.core.errors.UserFriendlyError.ValidationError
import app.mosia.domain.model.Email.Email
import app.mosia.domain.model.Email
import sttp.tapir.Schema
import zio.*
import zio.json.JsonCodec

import scala.util.Try

case class MagicLinkDto(email: String, token: String, clientNonce: Option[String] = None) derives JsonCodec, Schema:
  def toEmail: Task[Email] =
    ZIO.fromTry(Try(Email(email))).mapError(_ => ValidationError(info = "Invalid email format"))
