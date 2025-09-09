package app.mosia.interface.http.controllers.oauth

import app.mosia.application.dto.JwtDto
import app.mosia.core.configs.AppConfig
import app.mosia.core.errors.UserFriendlyError.*
import app.mosia.domain.model.input.ConnectedAccountInput
import app.mosia.domain.model.{Accounts, Users}
import app.mosia.infra.auth.{AuthService, AuthServiceImpl}
import app.mosia.infra.eventbus.EventBus
import app.mosia.infra.oauth.Types.*
import app.mosia.infra.oauth.{OAuthProviderFactory, OAuthProviderFactoryImpl, OAuthService, OAuthServiceImpl}
import app.mosia.infra.repository.RepoModule
import app.mosia.infra.repository.impl.RepoModuleImpl
import sttp.model.headers.CookieWithMeta
import sttp.tapir.model.ServerRequest
import zio.*
import zio.json.EncoderOps

import javax.sql.DataSource

case class OAuthControllerImpl(
  auth: AuthService,
  oauth: OAuthService,
  repo: RepoModule,
  factory: OAuthProviderFactory
) extends OAuthController:
  
  override def preflight(
    unknownProviderName: Option[String] = None,
    redirectUri: Option[String] = None,
    client: Option[String] = None,
    clientNonce: Option[String] = None
  ): Task[String] = unknownProviderName match {
    case Some(name) =>
      val providerName = OAuthProviderName.valueOf(name)
      factory.get(providerName).flatMap {
        case Some(provider) =>
          for {
            state <- oauth.saveOAuthState(
                       OAuthState(
                         redirectUri = redirectUri,
                         client = client,
                         clientNonce = clientNonce,
                         provider = providerName
                       )
                     )
            url   <- provider.getAuthUrl(
                       StateRequest(state = state, client = client, provider = providerName).toJson
                     )
          } yield url
        case None           =>
          ZIO.fail(
            UnknownOauthProvider(s"name $unknownProviderName", Some(providerName.toString))
          )
      }
    case None       => ZIO.fail(MissingOauthQueryParameter("name provider"))
  }

  override def callback(
    request: ServerRequest,
    code: Option[String] = None,
    stateStr: Option[String] = None,
    clientNonce: Option[String] = None
  ): Task[(JwtDto, List[CookieWithMeta])] = code match
    case Some(codeValue) =>
      stateStr match
        case Some(stateValue) =>
          for {
            isValid       <- oauth.isValidState(stateValue)
            _             <- ZIO.fail(InvalidOauthCallbackState()).unless(isValid)
            state         <- oauth.getOAuthState(stateValue).someOrFail(OauthStateExpired())
            _             <- ZIO.fail(InvalidAuthState()).when(state.clientNonce.exists(!clientNonce.contains(_)))
            providerName   = state.provider.toString
            provider      <- factory.get(state.provider).someOrFail(UnknownOauthProvider(providerName))
            tokens        <-
              provider
                .getToken(codeValue)
                .tapError(e =>
                  ZIO.logWarning(
                    s"Error getting oauth token for $providerName, callback code: $code, stateStr: $stateStr, error: ${e.getMessage}"
                  )
                )
            externAccount <- provider.getUser(tokens.accessToken)
            user          <- loginFromOauth(state.provider, externAccount, tokens)
            jwt            = JwtDto(s"""{"id": "${user.id.value}", "redirectUri": "${state.redirectUri}"}""")
            response      <- auth.setCookies(request = request, response = jwt, userId = user.id.value)
          } yield response
        case None             => ZIO.fail(MissingOauthQueryParameter("state"))
    case None            => ZIO.fail(MissingOauthQueryParameter("code"))

  private def loginFromOauth(
    provider: OAuthProviderName,
    externalAccount: OAuthAccount,
    tokens: Tokens
  ): Task[Users] =
    for
      connectedAccount <- repo.usersRepo.getConnectedAccount(provider, externalAccount.id)
      toEmail          <- externalAccount.toEmail
      user             <- connectedAccount match
                            case Some(account) =>
                              for {
                                _    <- updateConnectedAccount(account, tokens)
                                user <- repo.usersRepo.getUserById(account.userId).someOrFail(UserNotFound(account.userId.toString))
                              } yield user
                            case _             => repo.usersRepo.fullfill(toEmail, None, None)
    yield user

  private def updateConnectedAccount(
    connectedAccount: Accounts,
    tokens: Tokens
  ): Task[Accounts] =
    repo.usersRepo.updateConnectedAccount(
      connectedAccount.id.value,
      ConnectedAccountInput(
        userId = connectedAccount.userId,
        provider = connectedAccount.provider,
        providerAccountId = connectedAccount.providerAccountId,
        scope = tokens.scope,
        accessToken = Some(tokens.accessToken),
        refreshToken = tokens.refreshToken,
        expiresAt = tokens.expiresAt
      )
    )

object OAuthControllerImpl:
  def make: ZIO[OAuthProviderFactory & RepoModule & OAuthService & AuthService, Nothing, OAuthController] =
    for
      auth    <- ZIO.service[AuthService]
      oauth   <- ZIO.service[OAuthService]
      repo    <- ZIO.service[RepoModule]
      factory <- ZIO.service[OAuthProviderFactory]
    yield new OAuthControllerImpl(auth, oauth, repo, factory)

  val env: ZLayer[
    DataSource & Ref[AppConfig] & EventBus,
    Throwable,
    AuthService & OAuthService & RepoModule & OAuthProviderFactory
  ] =
    AuthServiceImpl.live ++
      OAuthServiceImpl.live ++
      RepoModuleImpl.layer ++
      OAuthProviderFactoryImpl.live

  val live: ZLayer[DataSource & Ref[AppConfig] & EventBus, Throwable, OAuthController] = env >>> ZLayer.fromZIO(make)

end OAuthControllerImpl
