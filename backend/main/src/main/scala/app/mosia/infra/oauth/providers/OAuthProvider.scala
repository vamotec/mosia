package app.mosia.infra.oauth.providers

import app.mosia.infra.oauth.Types.*
import zio.{ IO, Task }

// OAuth 认证提供者抽象类
trait OAuthProvider {
  def provider: OAuthProviderName

  def getAuthUrl(state: String): Task[String]

  def getToken(code: String): Task[Tokens]

  def getUser(token: String): Task[OAuthAccount]
}
