
// AUTO-GENERATED Quill data model
package app.mosia.models

import app.mosia.core.types.JSONValue
import io.getquill.*
import zio.json.*

case class DbMultipleUsersSessions(
  id: java.util.UUID, 
  createdAt: java.time.Instant
)

object DbMultipleUsersSessions:
    inline given schema: Quoted[EntityQuery[DbMultipleUsersSessions]] = quote:
      querySchema[DbMultipleUsersSessions]("public.multiple_users_sessions")

    given JsonEncoder[DbMultipleUsersSessions] = DeriveJsonEncoder.gen[DbMultipleUsersSessions]
    given JsonDecoder[DbMultipleUsersSessions] = DeriveJsonDecoder.gen[DbMultipleUsersSessions]
          
