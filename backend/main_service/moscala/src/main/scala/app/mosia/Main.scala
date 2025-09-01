package app.mosia

import app.mosia.core.configs.AppConfig
import app.mosia.core.kafka.KafkaLayers
import app.mosia.grpc.greeting.ZioGreeting.Greeter
import app.mosia.infra.auth.AuthServiceImpl
import app.mosia.infra.context.UserContext
import app.mosia.infra.dao.impl.QuillContext.dataSourceLayer
import app.mosia.infra.eventbus.{ EventBus, EventBusImpl }
import app.mosia.infra.helpers.extractor.CurrentUserExtractorImpl
import app.mosia.infra.jwt.JwtServiceImpl
import app.mosia.interface.grpc.GreeterImpl
import app.mosia.interface.http.api.ApiModule
import app.mosia.interface.http.controllers.ControllerModuleImpl
import app.mosia.interface.http.endpoints.RestEndpoints
import app.mosia.interface.middleware.*
import io.grpc.protobuf.services.ProtoReflectionService
import scalapb.zio_grpc.{ Server as GrpcServer, ServerLayer, ServiceList }
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.*
import zio.http.{ Server as HttpServer, * }
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.Middleware.{ cors, CorsConfig }
import zio.kafka.consumer.Consumer
import zio.kafka.producer.Producer
import zio.logging.backend.SLF4J

import javax.sql.DataSource

object Main extends ZIOAppDefault:
  // 定义logger
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.setConfigProvider(TypesafeConfigProvider.fromResourcePath()) >>> SLF4J.slf4j

  // 配置cors
  private val corsConfig: CorsConfig =
    CorsConfig(
      allowedOrigin = _ => Some(Header.AccessControlAllowOrigin.All), // 允许任意 Origin
      allowedMethods = Header.AccessControlAllowMethods.All,          // 允许所有方法
      allowedHeaders = Header.AccessControlAllowHeaders.All,          // 允许所有 Header（包括 X-Market）
      allowCredentials = Header.AccessControlAllowCredentials.Allow,  // 允许携带 cookies / headers
      exposedHeaders = Header.AccessControlExposeHeaders.All,         // 允许前端访问所有响应头
      maxAge = Some(Header.AccessControlMaxAge(1.hour))               // 预检请求缓存1小时
    )

  // 读取配置信息，设置服务器地址及端口
  private val httpServerConfigLayer =
    ZLayer.fromZIO:
      for
        appConfig <- ZIO.service[Ref[AppConfig]]
        cfg       <- appConfig.get
        serverCfg  = HttpServer.Config.default.binding(cfg.server.host, cfg.server.port)
      yield serverCfg

  private val producerLayer: ZLayer[Any, Throwable, Producer] = AppConfig.live >>> KafkaLayers.producerLayer

  private val consumerLayer: ZLayer[Any, Throwable, Consumer] = AppConfig.live >>> KafkaLayers.consumerLayer

  private val eventBusLayer: ZLayer[Any, Throwable, EventBus] = producerLayer ++ consumerLayer >>> EventBusImpl.live

  private val baseLayer = AppConfig.live ++ dataSourceLayer ++ eventBusLayer

  // 重定向 / to /docs
  private val redirectRootToDocs: Routes[Any, Nothing] =
    Routes(
      Method.GET / "" ->
        handler(Response.redirect(URL(path = Path.root / "docs"), isPermanent = true))
    )

  // 定义 gRPC 服务层
  private val grpcServerLayer = ServerLayer.fromServiceList(
    io.grpc.ServerBuilder
      .forPort(9090)
      .addService(ProtoReflectionService.newInstance()),
    ServiceList
      .addFromEnvironment[Greeter]
  )

  private val grpcServer: ZLayer[Any, Throwable, GrpcServer] = ZLayer.make[GrpcServer](
    baseLayer,
    grpcServerLayer,
    GreeterImpl.live
  )

  // 定义 api 服务层
  private val httpServer =
    for {
      apiModule        <- ZIO.service[ApiModule]
      restEndpoints     = RestEndpoints.make(
                            apiModule.authApi,
                            apiModule.oAuthApi
                          )
      businessEndpoints = restEndpoints
      docEndpoints      =
        SwaggerInterpreter().fromServerEndpoints(businessEndpoints, "Caliban tapir playground", "1.0")
      docRoutes         = ZioHttpInterpreter().toHttp[Any](docEndpoints)
      tapirRoutes       =
        ZioHttpInterpreter().toHttp[Any](businessEndpoints) @@
          AuthMiddleware.middleware @@
          LoggingMiddleware.middleware @@
          ErrorHandlingMiddleware.middleware @@
          cors(corsConfig)
      finalRoutes       = redirectRootToDocs ++ docRoutes ++ tapirRoutes
      _                <- HttpServer.serve(finalRoutes)
    } yield ()

  override val run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    ZIO.logInfo("Starting application") *>
      grpcServer.build *>
      httpServer
        .provide(
          baseLayer,
          JwtServiceImpl.live,
          ControllerModuleImpl.layer,
          CurrentUserExtractorImpl.live,
          httpServerConfigLayer,
          UserContext.live,
          ApiModule.live,
          HttpServer.live
        ) *> ZIO.never
