package app.mosia.domain.model

import app.mosia.core.types.JSONValue
import zio.json.JsonCodec

case class ConfigsPayload(json: JSONValue) derives JsonCodec
