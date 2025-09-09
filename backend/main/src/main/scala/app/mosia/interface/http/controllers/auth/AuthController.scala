package app.mosia.interface.http.controllers.auth

import app.mosia.application.dto.*
import app.mosia.domain.model.Email.Email
import sttp.model.headers.CookieWithMeta
import sttp.tapir.model.ServerRequest
import zio.*

trait AuthController:
  def signUp(request: ServerRequest, credential: CreateUserDto): Task[(JwtDto, List[CookieWithMeta])]

  def passwordSignIn(request: ServerRequest, loginDto: LoginDto): Task[(JwtDto, List[CookieWithMeta])]

  def signIn(
    request: ServerRequest,
    credential: OptionLoginDto,
    redirectUri: Option[String]
  ): Task[(JwtDto, List[CookieWithMeta])]

  def checkEmailStatus(email: Option[String]): Task[EmailCheckResponseDto]

  def sendMagicLink(
    _request: ServerRequest,
    email: Email,
    callbackUrl: String = "/magic-link",
    redirectUrl: Option[String] = None,
    clientNonce: Option[String] = None
  ): Task[(JwtDto, List[CookieWithMeta])]

  def signOut(
    responseData: MessageResponseDto,
    sessionId: Option[String] = None,
    userId: Option[String] = None
  ): Task[(MessageResponseDto, List[CookieWithMeta])]

  def magicLinkSignIn(
    request: ServerRequest,
    credential: MagicLinkDto
  ): Task[(JwtDto, List[CookieWithMeta])]

  def currentSessionUsers(request: ServerRequest): Task[String]
