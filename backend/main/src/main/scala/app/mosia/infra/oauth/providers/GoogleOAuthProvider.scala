package app.mosia.infra.oauth.providers

import app.mosia.core.errors.UserFriendlyError
import app.mosia.infra.oauth.Types.*
import sttp.client4.*
import sttp.client4.httpclient.zio.HttpClientZioBackend
import zio.json.{ DecoderOps, DeriveJsonDecoder, JsonCodec, JsonDecoder }
import zio.{ IO, Task, ZIO }

import java.net.URLEncoder
import java.time.Instant

case class GoogleOAuthProvider(config: OAuthProviderConfig, baseUrl: String) extends OAuthProvider:
  import GoogleOAuthProvider.*
  private val redirectUri = s"$baseUrl/api/oauth/callback"

  override def provider: OAuthProviderName = OAuthProviderName.Google

  override def getAuthUrl(state: String): IO[UserFriendlyError, String] = ZIO.succeed:
    val baseParams = Map(
      "client_id"    -> config.clientId,
      // 使用字符串插值确保URL格式正确
      "redirect_uri" -> redirectUri,
      "scope"        -> "openid email profile",
      "prompt"       -> "select_account",
      "access_type"  -> "offline",
      "state"        -> state
    )

    val allParams = baseParams ++ config.args.getOrElse(Map.empty)

    // 编码处理（推荐使用URIUtils，这里保持原有逻辑）
    val queryString = allParams.map { case (k, v) =>
      ZIO
        .attempt(
          s"${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        )
        .orDie
    }
      .mkString("&")
    // 返回链接
    s"https://accounts.google.com/o/oauth2/v2/auth?$queryString"

  override def getToken(code: String): Task[Tokens] =
    // 构建请求参数并编码
    val params   = Map(
      "code"          -> code,
      "client_id"     -> config.clientId,
      "client_secret" -> config.clientSecret,
      "redirect_uri"  -> redirectUri,
      "grant_type"    -> "authorization_code"
    )
    // 生成 x-www-form-urlencoded 请求体
    val formBody = params.map { case (k, v) =>
      s"${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
    }
      .mkString("&")
    // 创建 STTP 请求
    val request  = basicRequest
      .post(uri"https://oauth2.googleapis.com/token")
      .header("Accept", "application/json")
      .header("Content-Type", "application/x-www-form-urlencoded")
      .body(formBody)
    // 发送请求并处理响应
    for {
      backend   <- HttpClientZioBackend()
      response  <- request.send(backend)
      body      <- ZIO
                     .fromEither(response.body)
                     .mapError(err => new Exception(s"Response parsing failed: $err"))
      _         <- ZIO
                     .fail(new Exception(s"HTTP ${response.code}: ${response.statusText}"))
                     .when(!response.code.isSuccess)
      tokenResp <- ZIO
                     .fromEither(body.fromJson[GoogleTokenResponse])
                     .mapError(err => new Exception(s"JSON decoding failed: $err"))
      expiresAt  = Instant.now().plusSeconds(tokenResp.expires_in.toLong)
    } yield Tokens(
      accessToken = tokenResp.access_token,
      refreshToken = Some(tokenResp.refresh_token),
      scope = Some(tokenResp.scope),
      expiresAt = Some(expiresAt)
    )

  override def getUser(token: String): Task[OAuthAccount] =
    val request = basicRequest
      .get(uri"https://www.googleapis.com/oauth2/v2/userinfo")
      .header("Authorization", s"Bearer $token")

    for {
      backend  <- HttpClientZioBackend()
      response <- request.send(backend)
      body     <- ZIO
                    .fromEither(response.body)
                    .mapError(err => new Exception(s"Request failed: $err"))
      _        <- ZIO
                    .fail(new Exception(s"GitHub API error: ${response.code} ${response.statusText}"))
                    .when(!response.code.isSuccess)
      userInfo <- ZIO
                    .fromEither(body.fromJson[UserInfo])
                    .mapError(err => new Exception(s"JSON decoding failed: $err"))
    } yield OAuthAccount(
      id = userInfo.login,
      email = userInfo.email,
      avatarUrl = Some(userInfo.avatar_url)
    )

private object GoogleOAuthProvider:
  private case class GoogleTokenResponse(
    access_token: String,
    expires_in: Int,
    refresh_token: String,
    scope: String,
    token_type: String
  ) derives JsonCodec
