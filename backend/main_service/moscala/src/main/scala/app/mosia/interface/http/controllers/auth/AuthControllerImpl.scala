package app.mosia.interface.http.controllers.auth

import app.mosia.application.dto.*
import app.mosia.core.configs.{AppConfig, Env}
import app.mosia.core.errors.UserFriendlyError.*
import app.mosia.domain.model.Email.Email
import app.mosia.domain.model.{CurrentUser, Email}
import app.mosia.infra.auth.Types.sessionCookieName
import app.mosia.infra.auth.{AuthService, AuthServiceImpl}
import app.mosia.infra.cache.{CacheProvider, CacheProviderImpl, CacheSetOptions}
import app.mosia.infra.eventbus.EventBus
import app.mosia.infra.helpers.url.{URLHelper, URLHelperImpl}
import app.mosia.infra.jwt.{JwtService, JwtServiceImpl}
import app.mosia.infra.repository.RepoModule
import app.mosia.infra.repository.impl.RepoModuleImpl
import app.mosia.infra.token.TokenType
import app.mosia.mapper.IDConverter.StringToUUID
import sttp.model.headers.CookieWithMeta
import sttp.tapir.model.ServerRequest
import zio.*
import zio.json.EncoderOps

import java.security.SecureRandom
import javax.sql.DataSource
import scala.util.Try

case class AuthControllerImpl(
  auth: AuthService,
  repo: RepoModule,
  configRef: Ref[AppConfig],
  cache: CacheProvider,
  urlHelper: URLHelper,
  jwt: JwtService
) extends AuthController:
  import AuthControllerImpl.*
  
  override def signIn(
    request: ServerRequest,
    credential: OptionLoginDto,
    redirectUri: Option[String]
  ): Task[(JwtDto, List[CookieWithMeta])] =
    for
      canSignIn <- auth.canSignIn(credential.email)
      email     <- credential.toEmail
      result    <-
        if (!canSignIn)
          ZIO.fail(EarlyAccessRequired())
        else
          credential.password match {
            case Some(password) =>
              passwordSignIn(
                request,
                LoginDto(
                  credential.email,
                  password
                )
              )
            case None           =>
              sendMagicLink(
                request,
                email,
                credential.callbackUrl,
                redirectUri,
                credential.clientNonce
              )
          }
    yield result

  override def signUp(request: ServerRequest, credential: CreateUserDto): Task[(JwtDto, List[CookieWithMeta])] =
    for
      // 开发阶段用户限制，生产环境取消
      canSignIn <- auth.canSignIn(credential.email)
      _         <- ZIO.unless(canSignIn)(ZIO.fail(EarlyAccessRequired()))
      login     <- auth.register(credential)
      userId    <- login.user.id.asUUID
      response  <- auth.setCookies(request, JwtDto(login.token), userId)
    yield response

  override def passwordSignIn(request: ServerRequest, loginDto: LoginDto): Task[(JwtDto, List[CookieWithMeta])] =
    for
      login    <- auth.signIn(loginDto)
      userId   <- login.user.id.asUUID
      response <- auth.setCookies(request, JwtDto(login.token), userId)
    yield response

  override def checkEmailStatus(email: Option[String]): Task[EmailCheckResponseDto] =
    email match
      case Some(email) =>
        for
          toEmail <- ZIO.fromTry(Try(Email(email))).mapError(_ => ValidationError(info = "Invalid email format"))
          user    <- repo.usersRepo.getUserByEmail(toEmail)
          result  <- user match
                       case Some(u) =>
                         ZIO.succeed(
                           EmailCheckResponseDto(registered = u.registered, hasPassword = u.passwordHash.isDefined)
                         )
                       case None    => ZIO.succeed(EmailCheckResponseDto(registered = false, hasPassword = false))
        yield result
      case None        => ZIO.fail(InvalidEmail("email not provided"))

  // TODO: 发送验证码登陆的逻辑，后期再完善吧，现在暂时用不到
  override def sendMagicLink(
    _request: ServerRequest,
    email: Email,
    callbackUrl: String = "/magic-link",
    redirectUrl: Option[String] = None,
    clientNonce: Option[String] = None
  ): Task[(JwtDto, List[CookieWithMeta])] = for
    user      <- repo.usersRepo.getUserByEmail(email)
    config    <- configRef.get
    _         <- ZIO.when(user.isEmpty && !config.auth.allowSignup)(ZIO.fail(SignUpForbidden()))
    _         <- ZIO.when(user.isEmpty && config.auth.requireEmailDomainVerification) {
                   val parts = email.value.split('@').toList
                   for {
                     _ <- ZIO.when(parts.length != 2 || parts(1).isEmpty)(ZIO.fail(InvalidEmail(email.value)))
                     _ <- ZIO.when(parts.head.contains('+'))(ZIO.fail(InvalidEmail(email.value)))
                   } yield ()
                 }
    tokenId   <- repo.verifyTokenRepo.create(TokenType.SignIn, email.value, 30 * 60)
    // 生成验证码
    otp       <- otp()
    cacheKey   = s"magic-link-otp:$otp"
    _         <- cache.cache.set(
                   cacheKey,
                   Map("tokenId" -> tokenId, "clientNonce" -> clientNonce.getOrElse("")),
                   CacheSetOptions(Some(Duration.fromSeconds(30 * 60)))
                 )
    params     = Map(
                   "token" -> otp,
                   "email" -> email.value
                 ) ++ redirectUrl.map(url => Map("redirect_uri" -> url)).getOrElse(Map())
    magicLink <- urlHelper.link(callbackUrl, Some(params))
    _         <- ZIO.when(Env.dev)(
                   ZIO.logDebug(s"Magic link: $magicLink")
                 )
    context    = SignInEmailDto(
                   email = email.value,
                   code = magicLink,
                   userName = user.get.name
                 )
    _         <- auth.sendSignInEmail(context)
    response  <- auth.setCookies(_request, JwtDto(s"email: $email"), user.get.id.value)
  yield response

  override def signOut(
    responseData: MessageResponseDto,
    sessionId: Option[String] = None,
    userId: Option[String] = None
  ): Task[(MessageResponseDto, List[CookieWithMeta])] =
    for
      // 执行登出操作（如果 sessionId 存在）
      _                <- sessionId match {
                            case Some(id) => auth.signOut(id, userId)
                            case None     => ZIO.unit
                          }
      // 获取刷新后的 Cookie（假设 refreshCookies 返回 Set[CookieWithMeta]）
      refreshedCookies <- auth.refreshCookies(sessionCookieName, sessionId)
      cookies           = List(refreshedCookies)
    yield (responseData.copy(msg = responseData.msg + " - Signed out"), cookies)

  override def magicLinkSignIn(
    request: ServerRequest,
    credential: MagicLinkDto
  ): Task[(JwtDto, List[CookieWithMeta])] =
    for
      // 1. 校验参数
      _              <- ZIO.when(credential.token.isEmpty || credential.email.isEmpty)(ZIO.fail(EmailTokenNotFound()))
      toEmail        <- credential.toEmail
      // 2. 获取 token 并验证
      cacheKey        = s"magic-link-otp:$credential.token"
      cachedTokenOpt <- cache.cache.get[String](cacheKey)
      cachedToken    <- ZIO.fromOption(cachedTokenOpt).orElseFail(InvalidEmailToken())

      tokenRecord <- repo.verifyTokenRepo.verify(
                       TokenType.SignIn,
                       cachedToken,
                       credential = Some(credential.email),
                       keep = None
                     )
      _           <- ZIO.fromOption(tokenRecord).orElseFail(InvalidEmailToken())
      // 3. 获取用户
      user        <- repo.usersRepo.fullfill(toEmail, None, None)
      // 4. 设置 cookie
      response    <- auth.setCookies(request, JwtDto(credential.token), user.id.value)
    yield response

  override def currentSessionUsers(request: ServerRequest): Task[String] =
    for
      sessionId <- ZIO.succeed(
                     request.cookies.collectFirst {
                       case Right(c) if c.name == sessionCookieName => c.value
                     }
                   )
      result    <- sessionId match {
                     case Some(id) =>
                       for {
                         dtos        <- auth.getUserList(id)
                         currentList <- ZIO.foreach(dtos) { dto =>
                                          dto.toCurrentUser(sessionId)
                                        }
                         json         = currentList.toJson
                       } yield s"""{ "users": $json }"""
                     case None     => ZIO.succeed("""{ "users": [] }""")
                   }
    yield result

  private def randomInt(min: Int, max: Int): Task[Int] =
    val random = new SecureRandom
    ZIO.attempt {
      random.nextInt(max - min) + min
    }

  private def otp(length: Int = 6): Task[String] =
    for {
      digits <- ZIO.foreach((1 to length).toList)(_ => randomInt(0, 9))
    } yield digits.mkString

object AuthControllerImpl:
  def make: ZIO[
    URLHelper & CacheProvider & Ref[AppConfig] & RepoModule & AuthService & JwtService,
    Nothing,
    AuthController
  ] =
    for {
      auth      <- ZIO.service[AuthService]
      repo      <- ZIO.service[RepoModule]
      configRef <- ZIO.service[Ref[AppConfig]]
      cache     <- ZIO.service[CacheProvider]
      url       <- ZIO.service[URLHelper]
      jwt       <- ZIO.service[JwtService]
      controller = new AuthControllerImpl(auth, repo, configRef, cache, url, jwt)
    } yield controller

  val env: ZLayer[
    DataSource & Ref[AppConfig] & EventBus,
    Throwable,
    AuthService & RepoModule & URLHelper & CacheProvider & JwtService
  ] =
    AuthServiceImpl.live ++
      RepoModuleImpl.layer ++
      URLHelperImpl.live ++
      CacheProviderImpl.cacheCombine ++
      JwtServiceImpl.live

  val live: ZLayer[DataSource & Ref[AppConfig] & EventBus, Throwable, AuthController] = env >>> ZLayer.fromZIO(make)
