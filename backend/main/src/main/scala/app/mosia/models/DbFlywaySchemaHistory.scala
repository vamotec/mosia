
// AUTO-GENERATED Quill data model
package app.mosia.models

import app.mosia.core.types.JSONValue
import io.getquill.*
import zio.json.*

case class DbFlywaySchemaHistory(
  installedRank: Int, 
  version: Option[String], 
  description: String, 
  `type`: String, 
  script: String, 
  checksum: Option[Int], 
  installedBy: String, 
  installedOn: java.time.Instant, 
  executionTime: Int, 
  success: Boolean
)

object DbFlywaySchemaHistory:
    inline given schema: Quoted[EntityQuery[DbFlywaySchemaHistory]] = quote:
      querySchema[DbFlywaySchemaHistory]("public.flyway_schema_history")

    given JsonEncoder[DbFlywaySchemaHistory] = DeriveJsonEncoder.gen[DbFlywaySchemaHistory]
    given JsonDecoder[DbFlywaySchemaHistory] = DeriveJsonDecoder.gen[DbFlywaySchemaHistory]
          
