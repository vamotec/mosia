package app.mosia.core.errors

import io.grpc.{ Status, StatusException }

import java.sql.SQLException

object GrpcErrorMapper {
  def mapToGrpcError[E <: Throwable]: PartialFunction[E, StatusException] = {
    case e: IllegalArgumentException =>
      new StatusException(Status.INVALID_ARGUMENT.withDescription(e.getMessage))

    case e: SQLException =>
      new StatusException(Status.INTERNAL.withDescription(s"Database error: ${e.getMessage}"))

    case e: Throwable =>
      new StatusException(Status.INTERNAL.withDescription(s"Unexpected error: ${e.getMessage}"))
  }
}
