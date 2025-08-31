package app.mosia.infra.events

import app.mosia.core.configs.AppConfig
import zio.json.ast.Json
import zio.json.{ DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder }

sealed trait ConfigEvent
object ConfigEvent {
  final case class Changed(updates: AppConfig)          extends ConfigEvent
  final case class ChangedBroadcast(updates: AppConfig) extends ConfigEvent

  given JsonEncoder[Changed]          = DeriveJsonEncoder.gen[Changed]
  given JsonEncoder[ChangedBroadcast] = DeriveJsonEncoder.gen[ChangedBroadcast]

  given JsonDecoder[Changed]          = DeriveJsonDecoder.gen[Changed]
  given JsonDecoder[ChangedBroadcast] = DeriveJsonDecoder.gen[ChangedBroadcast]
}
