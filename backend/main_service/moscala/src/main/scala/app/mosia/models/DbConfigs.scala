
// AUTO-GENERATED Quill data model
package app.mosia.models

import app.mosia.core.types.JSONValue
import io.getquill.*
import zio.json.*

case class DbConfigs(
  id: String, 
  value: JSONValue, 
  createdAt: java.time.Instant, 
  updatedAt: java.time.Instant, 
  lastUpdatedBy: Option[java.util.UUID]
)

object DbConfigs:
    inline given schema: Quoted[EntityQuery[DbConfigs]] = quote:
      querySchema[DbConfigs]("public.configs")

    given JsonEncoder[DbConfigs] = DeriveJsonEncoder.gen[DbConfigs]
    given JsonDecoder[DbConfigs] = DeriveJsonDecoder.gen[DbConfigs]
          
