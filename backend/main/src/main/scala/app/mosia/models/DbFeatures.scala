
// AUTO-GENERATED Quill data model
package app.mosia.models

import app.mosia.core.types.JSONValue
import io.getquill.*
import zio.json.*

case class DbFeatures(
  id: Int, 
  feature: String, 
  configs: JSONValue, 
  updatedAt: java.time.Instant, 
  createdAt: java.time.Instant
)

object DbFeatures:
    inline given schema: Quoted[EntityQuery[DbFeatures]] = quote:
      querySchema[DbFeatures]("public.features")

    given JsonEncoder[DbFeatures] = DeriveJsonEncoder.gen[DbFeatures]
    given JsonDecoder[DbFeatures] = DeriveJsonDecoder.gen[DbFeatures]
          
