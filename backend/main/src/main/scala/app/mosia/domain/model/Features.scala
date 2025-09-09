package app.mosia.domain.model

import app.mosia.core.types.JSONValue
import zio.json.JsonCodec

case class Features(
  id: Int,
  name: String,
  configs: JSONValue
) derives JsonCodec
