package app.mosia.interface.middleware

import app.mosia.infra.context.UserContext
import app.mosia.infra.helpers.extractor.CurrentUserExtractor
import zio.ZIO
import zio.http.*

object AuthMiddleware:
  val middleware: Middleware[CurrentUserExtractor & UserContext] =
    new Middleware[CurrentUserExtractor & UserContext]:
      override def apply[Env1 <: CurrentUserExtractor & UserContext, Err](
        routes: Routes[Env1, Err]
      ): Routes[Env1, Err] =
        routes.transform: handler =>
          Handler.scoped:
            Handler.fromFunctionZIO[Request]: request =>
              for
                extractor <- ZIO.service[CurrentUserExtractor]
                user      <- extractor.getCurrent(request)
                ctx       <- ZIO.service[UserContext]
                _         <- ctx.set(user)
                response  <- handler(request)
              yield response
