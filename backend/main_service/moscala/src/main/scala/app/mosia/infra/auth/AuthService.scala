package app.mosia.infra.auth

import app.mosia.application.dto.*
import app.mosia.domain.model.{ CurrentUser, UserSessions, Users }
import app.mosia.infra.auth.Types.SessionOptions
import sttp.model.headers.CookieWithMeta
import sttp.tapir.model.ServerRequest
import zio.*
import zio.http.*

import java.util.UUID

trait AuthService:
  // ===== 基础认证方法 ===== //
  def canSignIn(email: String): Task[Boolean]
  def register(createUserDto: CreateUserDto): Task[LoginResponseDto]
  def signIn(loginDto: LoginDto): Task[LoginResponseDto]
  def signOut(sessionId: String, userId: Option[String] = None): Task[Long]
  // ===== Session 管理 ===== //
//  def getUserSession(
//    sessionId: UUID,
//    userId: Option[UUID] = None
//  ): Task[Option[UserResponseDto]]
//  def getUserSessions(sessionId: UUID): Task[List[UserSessionDto]]
//  def createUserSession(
//    userId: UUID,
//    sessionId: Option[UUID] = None
//  ): Task[UserSessionDto]
  def getUserList(sessionId: String): Task[List[UserResponseDto]]
  def refreshUserSessionIfNeeded(
    userSessions: UserSessionDto,
    ttr: Option[Duration] = None
  ): Task[Option[CookieWithMeta]]
//  def revokeUserSessions(userId: UUID): Task[Long]
  def getSessionOptionsFromRequest(request: ServerRequest): Task[SessionOptions]
  def getUserSessionFromRequest(
    request: ServerRequest
  ): Task[List[UserSessionDto]]
  // ===== Cookies 管理 ===== //
  def setCookies(
    request: ServerRequest,
    response: JwtDto,
    userId: UUID
  ): Task[(JwtDto, List[CookieWithMeta])]
  def refreshCookies(name: String, sessionId: Option[String] = None): Task[CookieWithMeta]
  def setUserCookie(userId: String): Task[CookieWithMeta]
  def getCookieValue(request: ServerRequest, key: String): Task[Option[String]]
  def getCookieUUID(request: ServerRequest, key: String): Task[Option[UUID]]
  // ===== 用户操作 ===== //
  def changePassword(id: String, changePasswordDto: ChangePasswordDto): Task[UserResponseDto]
  def setEmailVerified(id: String): Task[UserResponseDto]
  // ===== 邮件发送 ===== //
  def sendChangePasswordEmail(emailRequestDto: EmailRequestDto): Task[Boolean]
  def sendSetPasswordEmail(emailRequestDto: EmailRequestDto): Task[Boolean]
//  def sendChangeEmail(emailRequestDto: EmailRequestDto): Task[Boolean]
  def sendVerifyChangeEmail(emailRequestDto: EmailRequestDto): Task[Boolean]
//  def sendVerifyEmail(emailRequestDto: EmailRequestDto): Task[Boolean]
//  def sendNotificationChangeEmail(email: String): Task[Boolean]
  def sendSignInEmail(signInEmailDto: SignInEmailDto): Task[Boolean]
  // ===== 额外的便利方法 ===== //
//  def getCurrentUser(request: ServerRequest): Task[Option[CurrentUser]]
//  def validateSession(sessionId: String): Task[Boolean]
//  def getUserById(id: String): Task[Option[UserResponseDto]]
//  def getUserByEmail(email: String): Task[Option[UserResponseDto]]
//  def updateUserProfile(
//                         id: String,
//                         name: Option[String],
//                         avatarUrl: Option[String]
//                       ): Task[UserResponseDto]
