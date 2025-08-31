package app.mosia.infra.events

import app.mosia.domain.model.Users
import zio.json.{ DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder }

import java.util.UUID

sealed trait UserEvent
object UserEvent:
  final case class Created(user: Users)                           extends UserEvent
  final case class Updated(user: Users)                           extends UserEvent
  final case class Deleted(user: Users, workspaceIds: List[UUID]) extends UserEvent
  final case class PostCreated(user: Users)                       extends UserEvent

  given JsonEncoder[Created]     = DeriveJsonEncoder.gen[Created]
  given JsonEncoder[Updated]     = DeriveJsonEncoder.gen[Updated]
  given JsonEncoder[Deleted]     = DeriveJsonEncoder.gen[Deleted]
  given JsonEncoder[PostCreated] = DeriveJsonEncoder.gen[PostCreated]

  given JsonDecoder[Created]     = DeriveJsonDecoder.gen[Created]
  given JsonDecoder[Updated]     = DeriveJsonDecoder.gen[Updated]
  given JsonDecoder[Deleted]     = DeriveJsonDecoder.gen[Deleted]
  given JsonDecoder[PostCreated] = DeriveJsonDecoder.gen[PostCreated]
