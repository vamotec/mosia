package app.mosia.infra.auth

import app.mosia.domain.model.CurrentUser
import sttp.tapir.Schema
import zio.json.{ DeriveJsonCodec, DeriveJsonEncoder, JsonCodec, JsonEncoder }

import java.util.UUID

object Types:
  val sessionCookieName = "mosia_session_id"
  val userCookieName    = "mosia_user_id"
  val emailCookieName   = "mosia_email"

  case class ResponseData(message: String)
  given JsonCodec[ResponseData] = DeriveJsonCodec.gen[ResponseData]
  given Schema[ResponseData]    = Schema.derived

  case class SessionOptions(sessionId: Option[UUID], userId: Option[UUID])

  enum EarlyAccessType:
    case App, AI

  case class AuthResponse(token: String)
  case class AuthenticationFailed(message: String) extends Exception(message)
  case class EmailCheckResponse(registered: Boolean, hasPassword: Boolean)
  given JsonCodec[EmailCheckResponse] = DeriveJsonCodec.gen[EmailCheckResponse]
  given Schema[EmailCheckResponse]    = Schema.derived

  final case class SignInCredential(
    email: String,
    password: Option[String] = None,
    callbackUrl: String,
    clientNonce: Option[String] = None
  )
  given JsonCodec[SignInCredential] = DeriveJsonCodec.gen[SignInCredential]
  given Schema[SignInCredential]    = Schema.derived

  final case class SignUpCredential(
    email: String,
    password: String
  )
  given JsonCodec[SignUpCredential] = DeriveJsonCodec.gen[SignUpCredential]
  given Schema[SignUpCredential]    = Schema.derived

  final case class MagicLinkCredential(email: String, token: String, clientNonce: Option[String] = None)
  given JsonCodec[MagicLinkCredential] = DeriveJsonCodec.gen[MagicLinkCredential]
  given Schema[MagicLinkCredential]    = Schema.derived

  final case class PreflightParams(email: String)
  given JsonCodec[PreflightParams] = DeriveJsonCodec.gen[PreflightParams]
  given Schema[PreflightParams]    = Schema.derived

  case class SendMagicLinkResponse(email: String)
  given JsonEncoder[SendMagicLinkResponse] = DeriveJsonEncoder.gen[SendMagicLinkResponse]

  case class UsersList(list: Option[List[CurrentUser]])
