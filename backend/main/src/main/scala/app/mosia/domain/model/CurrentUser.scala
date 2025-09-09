package app.mosia.domain.model

import app.mosia.application.dto.JwtPayload
import app.mosia.core.errors.*
import app.mosia.domain.model.Email.Email
import app.mosia.domain.model.Id.UserId
import zio.*
import zio.json.JsonCodec

import java.time.{ Instant, LocalDateTime, OffsetDateTime, ZoneOffset }
import scala.util.Try
import java.util.UUID

case class CurrentUser(
  id: UserId,
  email: Email,
  name: String,
  avatarUrl: Option[String] = None,
  emailVerified: Boolean = false,
  sessionId: Option[String] = None,
  loginTime: LocalDateTime = LocalDateTime.now()
) derives JsonCodec:
  def hasVerifiedEmail: Boolean                              = emailVerified
  def canAccessResource(resourceOwnerId: UserId): Boolean    = id == resourceOwnerId
  def canAccessResource(resourceOwnerIdStr: String): Boolean =
    Try(UUID.fromString(resourceOwnerIdStr))
      .map(uuid => id == Id[Users](uuid))
      .getOrElse(false)

object CurrentUser:
  def fromJwtPayload(payload: JwtPayload, sessionId: Option[String] = None): IO[MapperError, CurrentUser] =
    for
      uuid  <- ZIO
                 .fromTry(Try(UUID.fromString(payload.sub)))
                 .mapError(ex => InvalidUserIdError(ex.getMessage))
      email <- ZIO
                 .fromTry(Try(Email(payload.email)))
                 .mapError(ex => InvalidEmailError(ex.getMessage))
    yield CurrentUser(
      id = Id[Users](uuid),
      email = email,
      name = payload.name,
      avatarUrl = payload.avatarUrl,
      emailVerified = payload.verified,
      sessionId = sessionId,
      loginTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(payload.iat), ZoneOffset.UTC)
    )

  def fromUser(user: Users, sessionId: Option[String] = None): UIO[CurrentUser] = ZIO.succeed:
    CurrentUser(
      id = user.id,
      email = user.email,
      name = user.name,
      avatarUrl = user.avatarUrl,
      emailVerified = user.isEmailVerified,
      sessionId = sessionId
    )

  private val GuestUserId: UUID = new UUID(0L, 0L)

  val guest: CurrentUser = CurrentUser(
    id = Id[Users](GuestUserId),
    email = Email("guest@anonymous.com"),
    name = "Guest User"
  )
