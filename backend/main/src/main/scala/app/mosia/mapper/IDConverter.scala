package app.mosia.mapper

import app.mosia.core.errors.UserFriendlyError.ValidationError
import zio.*

import java.util.UUID

object IDConverter:
  implicit class StringToUUID(id: String):
    def asUUID: Task[UUID] =
      ZIO
        .attempt(UUID.fromString(id))
        .mapError(ex => ValidationError(s"Invalid ID format: $id"))

  implicit class OptStringToUUID(id: Option[String]):
    def asUUIDOpt: Task[Option[UUID]] =
      ZIO.foreach(id): str =>
        ZIO
          .attempt(UUID.fromString(str))
          .mapError(ex => ValidationError(s"Invalid UUID format: '$str' - ${ex.getMessage}"))

  implicit class UUIDToString(uuid: UUID):
    def asString: String = uuid.toString
