package app.mosia.interface.middleware

import zio.*
import zio.http.*

object LoggingMiddleware:
  val middleware: Middleware[Any] =
    new Middleware[Any]:
      override def apply[Env1 <: Any, Err](routes: Routes[Env1, Err]): Routes[Env1, Err] =
        routes.transform: handler =>
          Handler.scoped:
            Handler.fromFunctionZIO[Request]: request =>
              for
                start    <- Clock.nanoTime
                _        <- ZIO.logInfo(s"➡️  ${request.method} ${request.url}")
                response <- handler(request)
                end      <- Clock.nanoTime
                elapsed   = (end - start) / 1_000_000 // 转换为毫秒
                _        <-
                  ZIO.logInfo(s"⬅️  ${request.method} ${request.url} responded with ${response.status} in ${elapsed}ms")
              yield response
