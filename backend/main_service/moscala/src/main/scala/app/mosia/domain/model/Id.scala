package app.mosia.domain.model

import zio.json.*

import java.util.UUID

final case class Id[T](value: UUID) extends AnyVal
object Id:
//  type AppConfigId     = Id[AppConfigs]
  type UserId          = Id[Users]
  type UserSessionId   = Id[UserSessions]
  type WorkspaceUserId = Id[WorkspaceUser]
  type SessionsId      = Id[Sessions]
  type AccountId       = Id[Accounts]

  given [T]: JsonEncoder[Id[T]] = implicitly[JsonEncoder[UUID]].contramap(_.value)
  given [T]: JsonDecoder[Id[T]] = implicitly[JsonDecoder[UUID]].map(Id[T])
