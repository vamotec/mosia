package app.mosia.infra.oauth

import app.mosia.core.errors.UserFriendlyError.ValidationError
import app.mosia.domain.model.Email
import app.mosia.domain.model.Email.Email
import caliban.schema.{ ArgBuilder, Schema }
import zio.*
import zio.json.*

import scala.util.Try

object Types:
  enum OAuthProviderName derives JsonCodec:
    case Google, GitHub, OIDC

  sealed trait ProviderConfig
  final case class OAuthProviderConfig(clientId: String, clientSecret: String, args: Option[Map[String, String]])
      extends ProviderConfig

  final case class OIDCArgs(
    scope: Option[String] = None,
    claimId: Option[String] = None,
    claimEmail: Option[String] = None,
    claimName: Option[String] = None
  )

  final case class OAuthOIDCProviderConfig(
    clientId: String,
    clientSecret: String,
    issuer: String,
    args: Option[OIDCArgs]
  ) extends ProviderConfig

  final case class OAuthConfig(providers: Map[OAuthProviderName, ProviderConfig])

  case class OAuthState(
    redirectUri: Option[String],
    client: Option[String],
    clientNonce: Option[String],
    provider: OAuthProviderName
  ) derives JsonCodec

  case class StateRequest(state: String, client: Option[String], provider: OAuthProviderName) derives JsonCodec

  // OAuth 账户信息
  final case class OAuthAccount(id: String, email: String, avatarUrl: Option[String] = None) derives JsonCodec:
    def toEmail: Task[Email] =
      ZIO.fromTry(Try(Email(email))).mapError(_ => ValidationError(info = "Invalid email format"))

  // OAuth 令牌信息
  final case class Tokens(
    accessToken: String,
    scope: Option[String] = None,
    refreshToken: Option[String] = None,
    expiresAt: Option[java.time.Instant] = None
  )

  case class UserInfo(login: String, email: String, avatar_url: String, name: String) derives JsonCodec
