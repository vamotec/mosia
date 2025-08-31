package app.mosia.interface.middleware

import zio.*
import zio.http.*

object ErrorHandlingMiddleware:
  val middleware: Middleware[Any] =
    new Middleware[Any] {
      override def apply[Env1 <: Any, Err](routes: Routes[Env1, Err]): Routes[Env1, Err] =
        routes.transform { handler =>
          Handler.scoped {
            Handler.fromFunctionZIO[Request] { request =>
              // 直接处理 handler(request) 的错误类型（Err = Response）
              handler(request).catchAll {
                case response: Response =>
                  response.status match
                    case Status.Ok | Status.Created | Status.Accepted =>
                      // 正常响应，直接返回
                      ZIO.succeed(response)
                    case status                                       =>
                      // 错误响应，记录日志
                      for
                        bodyText <- response.body.asString.orElse(ZIO.succeed("No body"))
                        _        <-
                          ZIO.logError(
                            s"❌ ${request.method} ${request.url} responded with error status $status: $bodyText"
                          )
                      yield response
                case null               =>
                  // 如果 Err 不是 Response（理论上不应该发生），返回默认错误
                  ZIO.logError(s"❌ ${request.method} ${request.url} failed with unexpected error") *>
                    ZIO.succeed(
                      Response
                        .text("Unexpected error")
                        .status(Status.InternalServerError)
                        .addHeader("X-Error-Type", "Unknown")
                    )
              }.catchAllCause { cause =>
                // 处理底层 Throwable（例如 ZIO 运行时异常）
                val message = cause.prettyPrint
                ZIO.logError(s"❌ ${request.method} ${request.url} failed with cause: $message") *>
                  ZIO.succeed(
                    Response
                      .text(s"Internal Server Error: $message")
                      .status(Status.InternalServerError)
                      .addHeader("X-Error-Type", cause.getClass.getSimpleName)
                  )
              }
            }
          }
        }
    }
