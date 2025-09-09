package app.mosia.infra.auth

import app.mosia.application.dto.*
import app.mosia.core.configs.AppConfig
import app.mosia.core.errors.*
import app.mosia.core.errors.UserFriendlyError.*
import app.mosia.domain.model.*
import app.mosia.domain.model.SessionResult.*
import app.mosia.domain.model.update.UsersUpdate
import app.mosia.infra.auth.Types.*
import app.mosia.infra.eventbus.EventBus
import app.mosia.infra.features.{FeatureService, FeatureServiceImpl}
import app.mosia.infra.jwt.{JwtService, JwtServiceImpl}
import app.mosia.infra.mailer.Mailer.*
import app.mosia.infra.mailer.{MailerService, MailerServiceImpl}
import app.mosia.infra.repository.RepoModule
import app.mosia.infra.repository.impl.RepoModuleImpl
import app.mosia.infra.token.{TokenService, TokenServiceImpl}
import app.mosia.mapper.DomainMappers.*
import app.mosia.mapper.DtoMappers.*
import app.mosia.mapper.IDConverter.*
import sttp.model.headers.Cookie.SameSite
import sttp.model.headers.CookieWithMeta
import sttp.tapir.model.*
import zio.json.*
import zio.{Duration, Ref, Task, ZEnvironment, ZIO, ZLayer}

import java.time.{Instant, OffsetDateTime}
import java.util.UUID
import javax.sql.DataSource
import scala.util.Try

case class AuthServiceImpl(
  repo: RepoModule,
  configRef: Ref[AppConfig],
  mailer: MailerService,
  tokenService: TokenService,
  features: FeatureService,
  jwt: JwtService,
  ds: DataSource
) extends AuthService:
  import AuthServiceImpl.*

  override def canSignIn(email: String): Task[Boolean] =
    for
      asEmail <- ZIO.fromTry(Try(Email(email))).mapError(_ => ValidationError(info = "Invalid email format"))
      result  <- features.canEarlyAccess(asEmail)
    yield result

  override def register(createUserDto: CreateUserDto): Task[LoginResponseDto] =
    for
      email        <- createUserDto.toEmail
      existingUser <- repo.usersRepo.getUserByEmail(email)
      _            <- ZIO.when(existingUser.isDefined)(ZIO.fail(EmailAlreadyUsed(email.value)))
      // 创建用户
      user         <- repo.usersRepo.create(email, createUserDto.name, createUserDto.password)
      token        <- jwt
                        .generateToken(user)
                        .mapError(ex => InvalidTokenError(ex.getMessage))
      // 计算token过期时间，假设token有效期是24小时
      expiresAt     = OffsetDateTime.now().plusHours(24)
      dto          <- user.toDto
    yield LoginResponseDto(
      user = dto,
      token = token,
      refreshToken = None, // 如果有refresh token逻辑，在这里设置
      expiresAt = expiresAt
    )

  // 有密码用户
  override def signIn(loginDto: LoginDto): Task[LoginResponseDto] =
    for
      email       <- loginDto.toEmail
      user        <- repo.usersRepo.signIn(email, loginDto.password)
      token       <- jwt
                       .generateToken(user)
                       .mapError(ex => InvalidTokenError(ex.getMessage))
      responseDto <- toDto(user)
                       .mapError(ex => ValidationError(ex.getMessage))
      // 计算token过期时间，假设token有效期是24小时
      expiresAt    = OffsetDateTime.now().plusHours(24)
    yield LoginResponseDto(
      user = responseDto,
      token = token,
      refreshToken = None, // 如果有refresh token逻辑，在这里设置
      expiresAt = expiresAt
    )

  override def signOut(sessionId: String, userId: Option[String]): Task[Long] =
    for
      sessionUuid <- sessionId.asUUID
      result      <- userId match {
                       case Some(u) =>
                         for
                           userUuid <- u.asUUID
                           count    <- repo.sessionRepo.deleteUserSession(userUuid, Some(sessionUuid))
                         yield count
                       case None    =>
                         repo.sessionRepo.deleteSession(sessionUuid)
                     }
    yield result

  private def getUserSession(sessionId: UUID, userId: Option[UUID]): Task[Option[UserResponseDto]] =
    for
      sessions     <- getUserSessions(sessionId)
      // 选择目标会话
      targetSession = userId.fold(sessions.headOption)(uid => sessions.find(_.userId == uid.toString))
      // 获取用户信息
      result       <- ZIO.foreach(targetSession): session =>
                        for
                          userUuid <- session.userId.asUUID
                          user     <- repo.usersRepo
                                        .getUserById(userUuid)
                                        .someOrFail(new RuntimeException(s"User ${session.userId} not found"))
                          userDto  <- user.toDto
                        yield userDto
    yield result

  private def getUserSessions(sessionId: UUID): Task[List[UserSessionDto]] =
    for
      result      <- repo.sessionRepo.findUserSessionsBySessionId(sessionId)
      withOutUser  = result.collect { case u: UserSessionWithOutUser => u }
      userSessions = withOutUser.map(_.session)
      dtos        <- userSessions.toDtoList
    yield dtos

  private def createUserSession(userId: UUID, sessionId: Option[UUID]): Task[UserSessionDto] =
    for
      userSession <- repo.sessionRepo.createOrRefreshUserSession(userId, sessionId)
      dto         <- userSession.toDto
    yield dto

  override def refreshUserSessionIfNeeded(
    userSessions: UserSessionDto,
    ttr: Option[Duration]
  ): Task[Option[CookieWithMeta]] =
    for
      domain       <- userSessions.toDomain
      newExpiresAt <- repo.sessionRepo.refreshUserSessionIfNeeded(domain, ttr)
      cookie       <- newExpiresAt match {
                        case Some(exp) =>
                          val maxAge = Some(Duration.fromInterval(userSessions.createdAt.toInstant, exp).getSeconds)
                          ZIO
                            .fromEither(
                              CookieWithMeta.safeApply(
                                name = sessionCookieName,
                                value = userSessions.sessionId,
                                expires = Some(exp),
                                maxAge = maxAge,
                                secure = true,
                                httpOnly = true,
                                sameSite = Some(SameSite.Strict)
                              )
                            )
                            .map(cookie => Some(cookie))
                            .mapError(error => new RuntimeException(s"Failed to create session cookie: $error"))
                        case None      => ZIO.succeed(None)
                      }
    yield cookie

  private def revokeUserSessions(userId: UUID): Task[Long] =
    for result <- repo.sessionRepo.deleteSession(userId)
    yield result

  override def getSessionOptionsFromRequest(request: ServerRequest): Task[SessionOptions] =
    for {
      // 从 Cookie 获取 sessionId
      sessionIdFromCookie <- getCookieUUID(request, sessionCookieName)
      // 如果 Cookie 中没有 sessionId，尝试从 Authorization 头提取
      sessionId           <- sessionIdFromCookie match {
                               case Some(id) => ZIO.succeed(Some(id))
                               case None     =>
                                 request.header("X-Session-Id") match {
                                   case Some(values) if values.nonEmpty =>
                                     ZIO.succeed(extractSessionIdFromHeader(values.head.toString))
                                   case _                               => ZIO.succeed(None)
                                 }
                             }
      // 从 Cookie 或 Header 获取 userId
      userIdFromCookie    <- getCookieUUID(request, userCookieName)
      userId              <- userIdFromCookie match {
                               case Some(id) => ZIO.succeed(Some(id))
                               case None     =>
                                 request.header("Authorization") match {
                                   case Some(authHeader) => extractUserIdFromHeader(authHeader)
                                   case None             => ZIO.succeed(None)
                                 }
                             }
    } yield SessionOptions(sessionId, userId)

  override def getUserSessionFromRequest(
    request: ServerRequest
  ): Task[List[UserSessionDto]] =
    for
      // 解析请求里的 session 选项
      sessionOptions <- getSessionOptionsFromRequest(request)

      // 查 session（可选）
      optSessions <-
        sessionOptions.sessionId match
          case None            => ZIO.none
          case Some(sessionId) =>
            getUserSessions(sessionId).map(Some(_)).catchAll(_ => ZIO.none)

      // 处理 cookie / 返回值
      sessions <- optSessions match
                    case Some(s) if s.nonEmpty =>
                      val userSession = s.head // 这里我假设第一个就是当前用户
                      for _ <-
                          ZIO.when(
                            sessionOptions.userId.isEmpty || sessionOptions.userId.get.toString != userSession.userId
                          ) {
                            ZIO.succeed(setUserCookie(userSession.userId))
                          }
                      yield s

                    case Some(_) =>
                      // 有 sessionId 但没有有效 session → 清掉 cookie
                      ZIO.succeed {
                        clearCookie(sessionCookieName)
                        Nil
                      }

                    case None =>
                      // 没有 sessionId → 返回空
                      ZIO.succeed(Nil)
    yield sessions

  override def setCookies(
    request: ServerRequest,
    response: JwtDto,
    userId: UUID
  ): Task[(JwtDto, List[CookieWithMeta])] =
    for {
      // 获取会话选项
      sessionOptions  <- getSessionOptionsFromRequest(request)
      // 使用 sessionId，如果不存在则提供默认值
      sessionId        = sessionOptions.sessionId
                           .getOrElse(java.util.UUID.randomUUID())
      // 创建用户会话
      userSession     <- createUserSession(userId, Some(sessionId))
      // 处理 expiresAt
      // 直接使用 expiresAt，因为它现在是必填的 OffsetDateTime
      createdAtInstant = userSession.createdAt.toInstant
      expiresAtInstant = userSession.expiresAt.toInstant
      // 设置会话 Cookie,这里先不去细化，调试过程中细化参数
      maxAge           = Some(Duration.fromInterval(createdAtInstant, expiresAtInstant).getSeconds)
      sessionCookie   <- ZIO
                           .fromEither(
                             CookieWithMeta.safeApply(
                               name = sessionCookieName,
                               value = userSession.sessionId,
                               expires = Some(expiresAtInstant),
                               maxAge = maxAge,
                               secure = true,
                               httpOnly = true,
                               sameSite = Some(SameSite.Strict)
                             )
                           )
                           .mapError(error => new RuntimeException(s"Failed to create session cookie: $error"))
      // 设置用户 Cookie
      userCookie      <- setUserCookie(userId.toString)
    } yield (response, List(sessionCookie, userCookie))

  override def refreshCookies(name: String, sessionId: Option[String] = None): Task[CookieWithMeta] =
    sessionId match {
      case Some(sid) =>
        getUserList(sid)
          .map(_.lastOption)
          .flatMap {
            case Some(user) => setUserCookie(user.id)
            case None       => clearCookie(name)
          }
      case None      =>
        clearCookie(name)
    }

  override def setUserCookie(userId: String): Task[CookieWithMeta] =
    ZIO
      .fromEither(
        CookieWithMeta.safeApply(
          name = userCookieName,
          value = userId,
          secure = true,
          httpOnly = true,
          sameSite = Some(SameSite.Strict)
        )
      )
      .mapError(error => new RuntimeException(s"Failed to create user cookie: $error"))

  override def getUserList(sessionId: String): Task[List[UserResponseDto]] =
    for {
      sessionUuid <- sessionId.asUUID
      sessions    <- repo.sessionRepo.findUserSessionsBySessionId(sessionUuid, Some(true))
      withUser     = sessions.collect { case u: UserSessionWithUser => u }
      users        = withUser.map(_.user)
      dtos        <- users.toDtoList
    } yield dtos

  override def getCookieValue(request: ServerRequest, key: String): Task[Option[String]] = ZIO.succeed(
    request.cookies.collectFirst {
      case Right(c) if c.name == key => c.value
    }
  )

  override def getCookieUUID(request: ServerRequest, key: String): Task[Option[UUID]] =
    ZIO.attempt {
      request.cookies.collectFirst {
        case Right(c) if c.name == key =>
          UUID.fromString(c.value)
      }
    }.catchSome { case _: IllegalArgumentException =>
      ZIO.succeed(None) // 非 UUID 字符串
    }

  override def changePassword(id: String, changePasswordDto: ChangePasswordDto): Task[UserResponseDto] =
    for
      uuid    <- id.asUUID
      password = Password.fromPlainText(changePasswordDto.newPassword)
      update   = UsersUpdate(password = Option(password))
      user    <- repo.usersRepo.update(id = uuid, data = update).provideEnvironment(ZEnvironment(ds))
      dto     <- user.toDto
    yield dto

  override def setEmailVerified(id: String): Task[UserResponseDto] =
    for
      uuid  <- id.asUUID
      update = UsersUpdate(emailVerified = Option(true))
      user  <- repo.usersRepo.update(id = uuid, data = update).provideEnvironment(ZEnvironment(ds))
      dto   <- user.toDto
    yield dto

  override def sendChangePasswordEmail(emailRequestDto: EmailRequestDto): Task[Boolean] = {
    for {
      config <- configRef.get

      // 1. 生成密码重置Token
      resetToken <- tokenService.generatePasswordResetToken(emailRequestDto.email)

      // 2. 构建重置链接
      baseUrl  = config.app.baseUrl
      resetUrl = s"$baseUrl/auth/reset-password?token=$resetToken"

      // 3. 准备邮件数据
      templateData = Map(
                       "email"        -> emailRequestDto.email,
                       "name"         -> emailRequestDto.userName.getOrElse("用户"),
                       "resetUrl"     -> resetUrl,
                       "expireHours"  -> "24", // Token有效期
                       "supportEmail" -> config.app.supportEmail.getOrElse("support@mosia.app")
                     )

      // 4. 创建邮件消息
      mail = EnhancedMail(
               to = emailRequestDto.email,
               subject = "密码重置请求",
               template = ChangePasswordTemplate,
               templateData = templateData,
               priority = High, // 密码重置是高优先级
               createdAt = Instant.now()
             )

      // 5. 发送到Kafka队列
      _ <- mailer.send(mailToMailMessage(mail))
      _ <- ZIO.logInfo(s"Password reset email queued for ${emailRequestDto.email}")

    } yield true
  }.catchAll { error =>
    ZIO.logError(s"Failed to send change password email: ${error.getMessage}") *>
      ZIO.succeed(false)
  }

  override def sendSetPasswordEmail(emailRequestDto: EmailRequestDto): Task[Boolean] = {
    for {
      config <- configRef.get

      // 1. 生成设置密码Token (新用户首次设置密码)
      setPasswordToken <- tokenService.generateSetPasswordToken(
                            emailRequestDto.email,
                            emailRequestDto.userId.getOrElse("")
                          )

      // 2. 构建设置密码链接
      baseUrl        = config.app.baseUrl
      setPasswordUrl = s"$baseUrl/auth/set-password?token=$setPasswordToken"

      // 3. 准备邮件数据
      templateData = Map(
                       "email"          -> emailRequestDto.email,
                       "name"           -> emailRequestDto.userName.getOrElse("新用户"),
                       "setPasswordUrl" -> setPasswordUrl,
                       "expireHours"    -> "72", // 新用户有更长的设置时间
                       "welcomeMessage" -> "欢迎加入我们的平台！",
                       "appName"        -> config.app.name.getOrElse("MosiaApp")
                     )

      // 4. 创建邮件消息
      mail = EnhancedMail(
               to = emailRequestDto.email,
               subject = "欢迎！请设置您的密码",
               template = SetPasswordTemplate,
               templateData = templateData,
               priority = High, // 新用户欢迎邮件也是高优先级
               createdAt = Instant.now()
             )

      // 5. 发送到Kafka队列
      _ <- mailer.send(mailToMailMessage(mail))
      _ <- ZIO.logInfo(s"Set password email queued for ${emailRequestDto.email}")

    } yield true
  }.catchAll { error =>
    ZIO.logError(s"Failed to send set password email: ${error.getMessage}") *>
      ZIO.succeed(false)
  }

  override def sendVerifyChangeEmail(emailRequestDto: EmailRequestDto): Task[Boolean] = {
    for {
      config <- configRef.get

      // 1. 生成邮箱验证Token
      verifyToken <- tokenService.generateEmailVerificationToken(
                       emailRequestDto.email,
                       emailRequestDto.userId.getOrElse("")
                     )

      // 2. 构建验证链接
      baseUrl   = config.app.baseUrl
      verifyUrl = s"$baseUrl/auth/verify-email?token=$verifyToken"

      // 3. 准备邮件数据
      templateData = Map(
                       "email"          -> emailRequestDto.email,
                       "name"           -> emailRequestDto.userName.getOrElse("用户"),
                       "verifyUrl"      -> verifyUrl,
                       "expireHours"    -> "24",
                       "securityNotice" -> "如果这不是您的操作，请立即联系我们的客服团队。"
                     )

      // 4. 创建邮件消息
      mail = EnhancedMail(
               to = emailRequestDto.email,
               subject = "验证您的新邮箱地址",
               template = VerifyChangeEmailTemplate,
               templateData = templateData,
               priority = Normal,
               createdAt = Instant.now()
             )

      // 5. 发送到Kafka队列
      _ <- mailer.send(mailToMailMessage(mail))
      _ <- ZIO.logInfo(s"Email verification queued for ${emailRequestDto.email}")

    } yield true
  }.catchAll { error =>
    ZIO.logError(s"Failed to send verify email: ${error.getMessage}") *>
      ZIO.succeed(false)
  }

  override def sendSignInEmail(signInEmailDto: SignInEmailDto): Task[Boolean] = {
    for
      config <- configRef.get
      // 1. 根据是否有验证码决定发送类型
      result <- if signInEmailDto.code.nonEmpty then sendSignInCodeEmail(signInEmailDto, config)
                else sendSignInNotificationEmail(signInEmailDto, config)
    yield result
  }.catchAll { error =>
    ZIO.logError(s"Failed to send sign in email: ${error.getMessage}") *>
      ZIO.succeed(false)
  }

  // 发送登录验证码邮件
  private def sendSignInCodeEmail(signInEmailDto: SignInEmailDto, config: AppConfig): Task[Boolean] =
    val templateData = Map(
      "email"         -> signInEmailDto.email,
      "name"          -> signInEmailDto.userName,
      "code"          -> signInEmailDto.code,
      "expireMinutes" -> "10", // 验证码10分钟过期
      "ipAddress"     -> signInEmailDto.ipAddress.getOrElse("未知"),
      "userAgent"     -> signInEmailDto.userAgent.getOrElse("未知浏览器"),
      "location"      -> signInEmailDto.location.getOrElse("未知位置"),
      "timestamp"     -> Instant.now().toString
    )
    // 创建邮件消息
    val mail         = EnhancedMail(
      to = signInEmailDto.email,
      subject = s"您的登录验证码：${signInEmailDto.code}",
      template = SignInCodeTemplate,
      templateData = templateData,
      priority = High, // 验证码邮件高优先级
      createdAt = Instant.now()
    )
    for
      // 发送到Kafka队列
      _ <- mailer.send(mailToMailMessage(mail))
      _ <- ZIO.logInfo(s"Sign in code email queued for ${signInEmailDto.email}")
    yield true

  // 发送登录通知邮件
  private def sendSignInNotificationEmail(signInEmailDto: SignInEmailDto, config: AppConfig): Task[Boolean] =
    // 准备邮件数据
    val templateData = Map(
      "email"       -> signInEmailDto.email,
      "name"        -> signInEmailDto.userName,
      "ipAddress"   -> signInEmailDto.ipAddress.getOrElse("未知"),
      "userAgent"   -> signInEmailDto.userAgent.getOrElse("未知浏览器"),
      "location"    -> signInEmailDto.location.getOrElse("未知位置"),
      "timestamp"   -> Instant.now().toString,
      "securityUrl" -> s"${config.app.baseUrl}/account/security"
    )
    // 创建邮件消息
    val mail         = EnhancedMail(
      to = signInEmailDto.email,
      subject = "账户登录通知",
      template = SignInNotificationTemplate,
      templateData = templateData,
      priority = Normal,
      createdAt = Instant.now()
    )
    for
      // 发送到Kafka队列
      _ <- mailer.send(mailToMailMessage(mail))
      _ <- ZIO.logInfo(s"Sign in notification email queued for ${signInEmailDto.email}")
    yield true

  // 将EnhancedMail转换为MailMessage (适配不同的邮件服务接口)
  private def mailToMailMessage(mail: EnhancedMail): MailMessage =
    MailMessage(
      id = UUID.randomUUID().toString,
      to = mail.to,
      subject = mail.subject,
      content = mail.toJson, // 将整个mail对象序列化
      priority = mail.priority match {
        case High   => "high"
        case Normal => "normal"
        case Low    => "low"
      },
      retryCount = mail.retryCount,
      maxRetries = mail.maxRetries,
      createdAt = mail.createdAt,
      scheduledAt = mail.scheduledAt
    )

  private def extractUserIdFromHeader(authHeader: String): Task[Option[UUID]] =
    authHeader.trim match
      case s if s.toLowerCase.startsWith("bearer ") =>
        val token = s.stripPrefix("Bearer ").stripPrefix("bearer ").trim
        (for
          payload <- jwt.validateToken(token)
          uuid    <- payload.sub.asUUID
        yield uuid)
          .map(Some(_))
          .catchAll(_ => ZIO.succeed(None)) // JWT验证失败或UUID解析失败都返回None
      case _                                        =>
        ZIO.succeed(None)

  private def extractSessionIdFromHeader(headerValue: String): Option[UUID] =
    scala.util.Try(UUID.fromString(headerValue.trim)).toOption

object AuthServiceImpl:
  private def clearCookie(name: String): Task[CookieWithMeta] =
    ZIO
      .fromEither(
        CookieWithMeta.safeApply(
          name = name,
          value = "",                      // 空值以清除 Cookie
          maxAge = Some(0),                // 立即过期
          secure = true,                   // 可选：保持安全性
          httpOnly = true,                 // 可选：防止客户端脚本访问
          sameSite = Some(SameSite.Strict) // 可选：增强 CSRF 防护
        )
      )
      .mapError(error => new RuntimeException(s"Failed to create clear cookie for $name: $error"))

  def make: ZIO[
    JwtService & MailerService & Ref[AppConfig] & RepoModule & DataSource & TokenService & FeatureService,
    Nothing,
    AuthService
  ] =
    for
      repo      <- ZIO.service[RepoModule]
      configRef <- ZIO.service[Ref[AppConfig]]
      mailer    <- ZIO.service[MailerService]
      token     <- ZIO.service[TokenService]
      features  <- ZIO.service[FeatureService]
      jwt       <- ZIO.service[JwtService]
      ds        <- ZIO.service[DataSource]
    yield new AuthServiceImpl(repo, configRef, mailer, token, features, jwt, ds)

  val env: ZLayer[
    DataSource & Ref[AppConfig] & EventBus,
    Throwable,
    RepoModule & MailerService & JwtService & TokenService & FeatureService
  ] =
    RepoModuleImpl.layer ++
      MailerServiceImpl.live ++
      FeatureServiceImpl.live ++
      JwtServiceImpl.live ++
      TokenServiceImpl.live

  val live: ZLayer[DataSource & Ref[AppConfig] & EventBus, Throwable, AuthService] = env >>> ZLayer.fromZIO(make)
