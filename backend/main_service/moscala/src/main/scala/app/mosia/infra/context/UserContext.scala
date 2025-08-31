package app.mosia.infra.context

import app.mosia.core.errors.UserFriendlyError
import caliban.CalibanError.ExecutionError
import UserFriendlyError.AuthenticationRequired
import app.mosia.domain.model.CurrentUser
import zio.*

trait UserContext:
  def get: Task[CurrentUser]
  def set(user: CurrentUser): UIO[Unit]

object UserContext:
  val live: ULayer[UserContext] = ZLayer.scoped:
    FiberRef
      .make[Option[CurrentUser]](None)
      .map: ref =>
        new UserContext:
          override def get: Task[CurrentUser] = ref.get.flatMap:
            case Some(u) => ZIO.succeed(u)
            case None    => ZIO.fail(AuthenticationRequired())

          override def set(user: CurrentUser): UIO[Unit] = ref.set(Some(user))

  def toExecutionError(e: Throwable): ExecutionError =
    ExecutionError(
      msg = e.getMessage,
      innerThrowable = Some(e)
    )
