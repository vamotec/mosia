package app.mosia.interface.grpc

import app.mosia.core.configs.AppConfig
import app.mosia.core.errors.GrpcErrorMapper.mapToGrpcError
import app.mosia.core.errors.UserFriendlyError.{ UserNotFound, ValidationError }
import app.mosia.domain.model.Email
import app.mosia.grpc.greeting.ZioGreeting.Greeter
import app.mosia.grpc.greeting.{ HelloReply, HelloRequest }
import app.mosia.infra.eventbus.EventBus
import app.mosia.infra.repository.RepoModule
import app.mosia.infra.repository.impl.RepoModuleImpl
import io.grpc.{ Status, StatusException }
import zio.*

import javax.sql.DataSource
import scala.util.Try

case class GreeterImpl(repo: RepoModule) extends Greeter:
  override def sayHello(
    request: HelloRequest
  ): ZIO[Any, StatusException, HelloReply] =
    for
      toEmail   <- ZIO.succeed(Email(request.email))
      maybeUser <- repo.usersRepo.getUserByEmail(toEmail).mapError(mapToGrpcError)
      user      <- maybeUser match {
                     case Some(u) => ZIO.succeed(u)
                     case None    => ZIO.fail(new StatusException(Status.NOT_FOUND.withDescription("User not found")))
                   }
      _         <- ZIO.logInfo(s"Found user: ${user.name}")
    yield HelloReply(s"Hello ${user.name}")

object GreeterImpl:
  val live: ZLayer[DataSource & Ref[AppConfig] & EventBus, Throwable, GreeterImpl] =
    RepoModuleImpl.layer >>> ZLayer.fromFunction(new GreeterImpl(_))
