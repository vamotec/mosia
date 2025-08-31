package app.mosia.mapper

import app.mosia.application.dto.{ UserResponseDto, UserSessionDto }
import app.mosia.domain.model.*
import io.scalaland.chimney.dsl.*
import zio.*

import java.time.format.DateTimeFormatter

object DtoMappers:
  trait ToDto[T, E]:
    extension (t: T) def toDto: Task[E]

  object ToDto:
    def apply[T, E](using ev: ToDto[T, E]): ToDto[T, E] = ev

  extension [T, E](dbList: List[T])
    def toDtoList(using ToDto[T, E]): Task[List[E]] =
      ZIO.foreach(dbList)(_.toDto)

  extension [T, E](value: T)(using toDomainEv: ToDto[T, E])
    def toDto: Task[E] =
      toDomainEv.toDto(value)

  private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  given ToDto[Users, UserResponseDto] with
    extension (domain: Users)
      def toDto: Task[UserResponseDto] =
        ZIO.attempt {
          domain
            .into[UserResponseDto]
            .withFieldComputed(_.id, d => Id[Users](d.id.value).toString)
            .withFieldComputed(_.email, d => d.email.value)
            .withFieldComputed(_.emailVerified, d => d.isEmailVerified)
            .withFieldComputed(_.createdAt, d => d.createdAt.format(dateFormatter))
            .transform
        }.mapError { e =>
          new RuntimeException(s"Failed to convert DbUserConnectedAccounts to domain: ${e.getMessage}", e)
        }

  given ToDto[UserSessions, UserSessionDto] with
    extension (domain: UserSessions)
      def toDto: Task[UserSessionDto] =
        ZIO.attempt {
          domain
            .into[UserSessionDto]
            .withFieldComputed(_.id, d => Id[UserSessions](d.id.value).toString)
            .withFieldComputed(_.userId, d => d.userId.toString)
            .withFieldComputed(_.sessionId, d => d.sessionId.toString)
            .transform
        }.mapError { e =>
          new RuntimeException(s"Failed to convert DbUserConnectedAccounts to domain: ${e.getMessage}", e)
        }
