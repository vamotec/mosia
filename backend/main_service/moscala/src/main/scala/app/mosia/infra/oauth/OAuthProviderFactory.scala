package app.mosia.infra.oauth

import app.mosia.core.errors.UserFriendlyError
import app.mosia.infra.oauth.Types.*
import app.mosia.infra.oauth.providers.OAuthProvider
import zio.{ IO, Task }

trait OAuthProviderFactory {
  def provider: Task[List[OAuthProviderName]]
  // 获取提供者
  def get(name: OAuthProviderName): Task[Option[OAuthProvider]]
  // 注册提供者
  def register(name: OAuthProviderName, provider: OAuthProvider): Task[Unit]
  // 移除提供者
  def unregister(provider: OAuthProvider): Task[Unit]
}
