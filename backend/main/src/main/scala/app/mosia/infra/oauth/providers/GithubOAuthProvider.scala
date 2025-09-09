package app.mosia.infra.oauth.providers

import app.mosia.core.errors.UserFriendlyError
import app.mosia.infra.oauth.Types.*
import sttp.client4.*
import sttp.client4.httpclient.zio.HttpClientZioBackend
import zio.*
import zio.json.{ DecoderOps, DeriveJsonDecoder, JsonCodec, JsonDecoder }

import java.net.URLEncoder
import java.time.Instant

final case class GithubOAuthProvider(config: OAuthProviderConfig, baseUrl: String) extends OAuthProvider:
  import GithubOAuthProvider.*
  private val redirectUri = s"$baseUrl/api/oauth/callback"

  override def provider: OAuthProviderName = OAuthProviderName.GitHub

  override def getAuthUrl(state: String): IO[UserFriendlyError, String] = ZIO.succeed:
    // 使用不可变Map代替可变Map
    val baseParams  = Map(
      "client_id"    -> config.clientId,
      // 使用字符串插值确保URL格式正确
      "redirect_uri" -> redirectUri,
      "scope"        -> "user",
      "state"        -> state
    )
    // 合并参数（假设config.args是Option[Map[String, String]]）
    val allParams   = baseParams ++ config.args.getOrElse(Map.empty)
    // 编码处理（推荐使用URIUtils，这里保持原有逻辑）
    val queryString = allParams.map { case (k, v) =>
      ZIO
        .attempt(
          s"${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        )
        .orDie
    }
      .mkString("&")
    s"https://github.com/login/oauth/authorize?$queryString"

  override def getToken(code: String): Task[Tokens] =
    // 构建请求参数并编码
    val params   = Map(
      "code"          -> code,
      "client_id"     -> config.clientId,
      "client_secret" -> config.clientSecret,
      "redirect_uri"  -> redirectUri
    )
    // 生成 x-www-form-urlencoded 请求体
    val formBody = params.map { case (k, v) =>
      s"${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
    }
      .mkString("&")
    // 创建 STTP 请求
    val request  = basicRequest
      .post(uri"https://github.com/login/oauth/access_token")
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
                     .fromEither(body.fromJson[GithubTokenResponse])
                     .mapError(err => new Exception(s"JSON decoding failed: $err"))
      expiresIn  = 3600L // 如果从 tokenResp 中能取出 expires_in 就替换这句
      expiresAt  = Instant.now().plusSeconds(expiresIn)
    } yield Tokens(
      accessToken = tokenResp.access_token,
      scope = Some(tokenResp.scope),
      expiresAt = Some(expiresAt)
    )

  override def getUser(token: String): Task[OAuthAccount] =
    val request = basicRequest
      .get(uri"https://api.github.com/user")
      .header("Authorization", s"Bearer $token")
    for {
      backend  <- HttpClientZioBackend()
      response <- request.send(backend)
      rawBody  <- ZIO
                    .fromEither(response.body)
                    .mapError(err => new Exception(s"Request failed: $err"))
      _        <- ZIO
                    .fail(new Exception(s"GitHub API error: ${response.code} ${response.statusText}"))
                    .when(!response.code.isSuccess)
      userInfo <- ZIO
                    .fromEither(rawBody.fromJson[UserInfo])
                    .mapError(err => new Exception(s"Failed to parse user info: $err"))
    } yield OAuthAccount(
      id = userInfo.login,
      email = userInfo.email,
      avatarUrl = Some(userInfo.avatar_url)
    )

private object GithubOAuthProvider:
  private case class GithubTokenResponse(access_token: String, scope: String, token_type: String) derives JsonCodec
