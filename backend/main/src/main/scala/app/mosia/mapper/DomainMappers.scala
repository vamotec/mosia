package app.mosia.mapper

import app.mosia.application.dto.UserSessionDto
import app.mosia.domain.model.*
import app.mosia.mapper.IDConverter.StringToUUID
import app.mosia.models.*
import io.scalaland.chimney.dsl.*
import zio.{ Task, ZIO }

import java.time.format.DateTimeFormatter
import java.time.*

object DomainMappers:
  trait ToDomain[T, E]:
    extension (t: T) def toDomain: Task[E]

  object ToDomain:
    def apply[T, E](using ev: ToDomain[T, E]): ToDomain[T, E] = ev

  extension [T, E](dbList: List[T])
    def toDomainList(using ToDomain[T, E]): Task[List[E]] =
      ZIO.foreach(dbList)(_.toDomain)

  extension [T, E](value: T)(using toDomainEv: ToDomain[T, E])
    def toDomain: Task[E] =
      toDomainEv.toDomain(value)

  /**
   * Utils
   */
  def localDateToInstant(ld: LocalDate): Instant =
    ld.atStartOfDay(ZoneOffset.UTC).toInstant

  def instantToLocalDate(ins: Instant): LocalDate =
    ins.atZone(ZoneOffset.UTC).toLocalDate

  /**
   * Db -> Domain
   */
  given ToDomain[DbUsers, Users] with
    extension (db: DbUsers)
      def toDomain: Task[Users] =
        for domain <- ZIO.attempt {
                        db.into[Users]
                          .withFieldComputed(_.id, d => Id[Users](d.id))
                          .withFieldComputed(_.email, d => Email(d.email))
                          .withFieldComputed(_.passwordHash, d => d.passwordHash.map(Password.fromHash))
                          .withFieldComputed(
                            _.createdAt,
                            d => d.createdAt.atOffset(ZoneOffset.UTC) // 防止None
                          )
                          .withFieldComputed(
                            _.updatedAt,
                            d => d.updatedAt.atOffset(ZoneOffset.UTC)
                          )
                          .withFieldComputed(
                            _.emailVerified,
                            d => d.emailVerified.map(_.atOffset(ZoneOffset.UTC))
                          )
                          .transform
                      }
        yield domain

  given ToDomain[DbFeatures, Features] with
    extension (db: DbFeatures)
      def toDomain: Task[Features] =
        ZIO.attempt {
          db.into[Features].withFieldRenamed(_.feature, _.name).transform
        }.mapError { e =>
          new RuntimeException(s"Failed to convert DbFeatures to domain: ${e.getMessage}", e)
        }

  given ToDomain[DbUserSessions, UserSessions] with
    extension (db: DbUserSessions)
      def toDomain: Task[UserSessions] =
        ZIO.attempt {
          db
            .into[UserSessions]
            .withFieldComputed(_.id, d => Id[UserSessions](d.id))
            .withFieldComputed(
              _.lastAccessedAt,
              d => d.lastAccessedAt.atOffset(ZoneOffset.UTC) // 防止None
            )
            .withFieldComputed(
              _.expiresAt,
              d => d.expiresAt.atOffset(ZoneOffset.UTC) // 防止None
            )
            .withFieldComputed(
              _.createdAt,
              d => d.createdAt.atOffset(ZoneOffset.UTC) // 防止None
            )
            .transform
        }.mapError { e =>
          new RuntimeException(s"Failed to convert DbUserSessions to domain: ${e.getMessage}", e)
        }

  given ToDomain[DbUserFeatures, UserFeatures] with
    extension (db: DbUserFeatures)
      def toDomain: Task[UserFeatures] =
        ZIO.attempt {
          db.into[UserFeatures].transform
        }.mapError { e =>
          new RuntimeException(s"Failed to convert DbUserFeatures to domain: ${e.getMessage}", e)
        }

  given ToDomain[DbWorkspaceUserPermissions, WorkspaceUser] with
    extension (db: DbWorkspaceUserPermissions)
      def toDomain: Task[WorkspaceUser] =
        ZIO.attempt {
          db
            .into[WorkspaceUser]
            .withFieldComputed(_.id, d => Id[WorkspaceUser](d.id))
            .transform
        }.mapError { e =>
          new RuntimeException(s"Failed to convert DbWorkspaceUserPermissions to domain: ${e.getMessage}", e)
        }

  given ToDomain[DbConfigs, Configs] with
    extension (db: DbConfigs)
      def toDomain: Task[Configs] =
        ZIO.attempt {
          db
            .into[Configs]
            .withFieldComputed(_.config, db => ConfigsPayload(db.value))
            .transform
        }.mapError { e =>
          new RuntimeException(s"Failed to convert DbAppConfigs to domain: ${e.getMessage}", e)
        }

  given ToDomain[DbVerifyTokens, VerifyTokens] with
    extension (db: DbVerifyTokens)
      def toDomain: Task[VerifyTokens] =
        ZIO.attempt {
          db
            .into[VerifyTokens]
            .transform
        }.mapError { e =>
          new RuntimeException(s"Failed to convert DbAppConfigs to domain: ${e.getMessage}", e)
        }

  given ToDomain[DbMultipleUsersSessions, Sessions] with
    extension (db: DbMultipleUsersSessions)
      def toDomain: Task[Sessions] =
        ZIO.attempt {
          db
            .into[Sessions]
            .withFieldComputed(_.id, d => Id[Sessions](d.id))
            .transform
        }.mapError { e =>
          new RuntimeException(s"Failed to convert DbMultipleUsersSessions to domain: ${e.getMessage}", e)
        }

  given ToDomain[DbUserConnectedAccounts, Accounts] with
    extension (db: DbUserConnectedAccounts)
      def toDomain: Task[Accounts] =
        ZIO.attempt {
          db
            .into[Accounts]
            .withFieldComputed(_.id, d => Id[Accounts](d.id))
            .transform
        }.mapError { e =>
          new RuntimeException(s"Failed to convert DbUserConnectedAccounts to domain: ${e.getMessage}", e)
        }

  /**
   * Dto -> Domain
   */
  given ToDomain[UserSessionDto, UserSessions] with
    extension (dto: UserSessionDto)
      def toDomain: Task[UserSessions] =
        for
          uuid        <- dto.id.asUUID
          userUuid    <- dto.userId.asUUID
          sessionUuid <- dto.sessionId.asUUID
          result      <- ZIO.attempt:
                           dto
                             .into[UserSessions]
                             .withFieldComputed(_.id, d => Id[UserSessions](uuid))
                             .withFieldConst(_.userId, userUuid)
                             .withFieldConst(_.sessionId, sessionUuid)
                             .transform
        yield result
