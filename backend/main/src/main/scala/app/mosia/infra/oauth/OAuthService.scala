package app.mosia.infra.oauth

import app.mosia.infra.oauth.Types.*
import zio.*
import zio.redis.RedisError

trait OAuthService:
  def isValidState(stateStr: String): Task[Boolean]

  def saveOAuthState(state: OAuthState): IO[RedisError, String]

  def getOAuthState(token: String): Task[Option[OAuthState]]

  def availableOAuthProviders(name: String): Task[OAuthProviderName]
