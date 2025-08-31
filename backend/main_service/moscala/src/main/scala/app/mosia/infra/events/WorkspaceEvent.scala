package app.mosia.infra.events

import zio.json.{ DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder }

import java.util.UUID

sealed trait WorkspaceEvent
object WorkspaceEvent {
  final case class BlobSync(workspaceId: UUID, key: String)              extends WorkspaceEvent
  final case class Deleted(id: UUID)                                     extends WorkspaceEvent
  final case class BlobDelete(workspaceId: UUID, key: String)            extends WorkspaceEvent
  final case class OwnerChanged(workspaceId: UUID, from: UUID, to: UUID) extends WorkspaceEvent

  given JsonEncoder[BlobSync]     = DeriveJsonEncoder.gen[BlobSync]
  given JsonEncoder[BlobDelete]   = DeriveJsonEncoder.gen[BlobDelete]
  given JsonEncoder[Deleted]      = DeriveJsonEncoder.gen[Deleted]
  given JsonEncoder[OwnerChanged] = DeriveJsonEncoder.gen[OwnerChanged]

  given JsonDecoder[BlobSync]     = DeriveJsonDecoder.gen[BlobSync]
  given JsonDecoder[BlobDelete]   = DeriveJsonDecoder.gen[BlobDelete]
  given JsonDecoder[Deleted]      = DeriveJsonDecoder.gen[Deleted]
  given JsonDecoder[OwnerChanged] = DeriveJsonDecoder.gen[OwnerChanged]
}
