package app.mosia.infra.helpers.url

import app.mosia.core.configs.AppConfig
import app.mosia.infra.eventbus.EventBus
import app.mosia.infra.events.ConfigEvent
import zio.*
import zio.http.*

import java.net.{ URI, URLEncoder }

case class URLHelperImpl(
  configRef: Ref[AppConfig],
  baseUrlRef: Ref[URI],
  originRef: Ref[URL],
  eventBus: EventBus,
  redirectAllowHostsRef: Ref[List[URI]],
  homeRef: Ref[String]
) extends URLHelper {
  import URLHelperImpl.*
  override def init(): Task[Unit] =
    for {
      config                 <- configRef.get
      tuple                  <- computeOriginAndBaseUrl(config)
      (originStr, baseUrlStr) = tuple
      origin                 <- ZIO
                                  .fromEither(URL.decode(originStr))
                                  .mapError(e => new IllegalArgumentException("Invalid origin URL", e))
      baseUrlUri             <- ZIO.attempt(new URI(baseUrlStr))
      _                      <- baseUrlRef.set(baseUrlUri)
      _                      <- originRef.set(origin)
      _                      <- redirectAllowHostsRef.set(List(baseUrlUri))
      _                      <- homeRef.set(baseUrlStr)
    } yield ()

  override def getBaseUrl: UIO[String] = ZIO.succeed(baseUrlRef.get.toString)

  override def stringify(query: (String, String)): Task[String] =
    ZIO.attempt(QueryParams(query).encode)

  override def addSimpleQuery(
    urlStr: String,
    key: String,
    value: String,
    escape: Boolean = true
  ): Task[String] =
    for {
      url        <- ZIO.fromEither(URL.decode(urlStr).left.map(new Exception(_)))
      updatedUrl <-
        if (escape) {
          val encodedValue = URLEncoder.encode(value, "UTF-8")
          val newParams    = url.queryParams ++ QueryParams(key -> encodedValue)
          ZIO.succeed(url.copy(queryParams = newParams))
        } else {
          // 手动拼接 query 字符串
          val base        = s"${url.scheme.getOrElse("http")}://${url.host}${url.path}"
          val sep         = if (url.queryParams.isEmpty) "?" else "&"
          val queryString = s"$sep$key=$value"
          ZIO.succeed(URL.decode(base + queryString).toOption.get)
        }
    } yield updatedUrl.encode

  override def url(path: String, query: Option[Map[String, String]]): Task[URL] =
    for {
      origin <- originRef.get
      result <- ZIO.attempt {
                  val newPath     = origin.path ++ Path.decode(path)
                  val queryParams = toQueryParams(query.get)
                  origin.copy(path = newPath, queryParams = queryParams)
                }
    } yield result

  override def link(path: String, query: Option[Map[String, String]] = None): Task[String] =
    ZIO.attempt(url(path, query).toString)

  override def safeRedirect(response: Response, to: String): Task[Response] =
    for {
      home               <- homeRef.get
      baseUrl            <- baseUrlRef.get
      redirectAllowHosts <- redirectAllowHostsRef.get
      homeUrl             = URL.decode(home).toOption.getOrElse(URL(path = Path("/")))
      fallback            = Response.redirect(homeUrl)
      result             <- ZIO.attempt {
                              val decodedTo = java.net.URLDecoder.decode(to, "UTF-8")
                              val finalTo   = baseUrl.resolve(decodedTo)

                              val matched = redirectAllowHosts.exists { host =>
                                val sameOrigin = Option(host.getScheme).contains(finalTo.getScheme) &&
                                  Option(host.getHost).contains(finalTo.getHost) &&
                                  host.getPort == finalTo.getPort
                                val pathOk     = Option(finalTo.getPath).exists(_.startsWith(host.getPath))
                                sameOrigin && pathOk
                              }

                              if (matched) Some(finalTo) else None
                            }.flatMap {
                              case Some(uri) =>
                                UriToURL(uri).map(Response.redirect(_)).catchAll(_ => ZIO.succeed(fallback))
                              case None      => ZIO.succeed(fallback)
                            }.catchAll(_ => ZIO.succeed(fallback))
    } yield result

  override def verify(url: String): Task[Boolean] =
    ZIO
      .attempt(new URI(url))
      .map { uri =>
        val scheme = Option(uri.getScheme).getOrElse("")
        val host   = Option(uri.getHost).getOrElse("")
        (scheme == "http" || scheme == "https") && host.nonEmpty
      }
      .catchAll(_ => ZIO.succeed(false))

  override def registerListeners(): UIO[Unit] = for {
    _ <- eventBus.onEvent[ConfigEvent.Changed] {
           case ConfigEvent.Changed(updates) =>
             for {
               _ <- configRef.set(updates)
               _ <- init()
               _ <- ZIO.logInfo("URLService reinitialized after config change")
             } yield ()
           case null                         => ZIO.unit
         }
  } yield ()
}

object URLHelperImpl {
  private def stringToURL(uriStr: String): IO[Throwable, URL] =
    ZIO.attempt {
      val uri = new URI(uriStr)
      URL.decode(uri.toString) match {
        case Left(error)  => throw new IllegalArgumentException(s"Invalid URL: $error")
        case Right(value) => value
      }
    }

  private def UriToURL(uri: URI): IO[Throwable, URL] =
    stringToURL(uri.toString)

  private def toQueryParams(query: Map[String, String]): QueryParams =
    QueryParams(query.head, query.toSeq: _*)

  private def computeOriginAndBaseUrl(config: AppConfig): Task[(String, String)] =
    config.server.externalUrl match {
      case Some(url) =>
        val uri       = new URI(url)
        val originStr = s"${uri.getScheme}://${uri.getAuthority}"
        val path      = Option(uri.getPath).getOrElse("")
        val basePath  = if (path.endsWith("/")) path.dropRight(1) else path
        ZIO.succeed((originStr, originStr + basePath))

      case None =>
        val originStr  = {
          val scheme   = if (config.server.https) "https" else "http"
          val host     = config.server.host
          val portPart =
            if (host == "localhost" || java.net.InetAddress.getByName(host).isSiteLocalAddress)
              s":${config.server.port}"
            else ""
          s"$scheme://$host$portPart"
        }
        val baseUrlStr = originStr + config.server.path
        ZIO.succeed((originStr, baseUrlStr))
    }

  def make(configRef: Ref[AppConfig], eventBus: EventBus): ZIO[Any, Throwable, URLHelper] = for {
    baseUrlRef            <- Ref.make[URI](new URI(""))
    originRef             <- Ref.make[URL](URL(path = Path("/")))
    redirectAllowHostsRef <- Ref.make[List[URI]](List.empty)
    homeRef               <- Ref.make[String]("")
    storage                = new URLHelperImpl(configRef, baseUrlRef, originRef, eventBus, redirectAllowHostsRef, homeRef)
    _                     <- storage.init()
    _                     <- storage.registerListeners()
  } yield storage

  val live: ZLayer[EventBus with Ref[AppConfig], Throwable, URLHelper] = ZLayer {
    for {
      configRef <- ZIO.service[Ref[AppConfig]]
      eventBus  <- ZIO.service[EventBus]
      service   <- make(configRef, eventBus)
    } yield service
  }
}
