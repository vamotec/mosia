
// AUTO-GENERATED Quill data model
package app.mosia.models

import app.mosia.core.types.JSONValue
import io.getquill.*
import zio.json.*

case class DbUserSessions(
  id: java.util.UUID, 
  sessionId: java.util.UUID, 
  userId: java.util.UUID, 
  lastAccessedAt: java.time.Instant, 
  expiresAt: java.time.Instant, 
  createdAt: java.time.Instant, 
  isActive: Boolean
)

object DbUserSessions:
    inline given schema: Quoted[EntityQuery[DbUserSessions]] = quote:
      querySchema[DbUserSessions]("public.user_sessions")

    given JsonEncoder[DbUserSessions] = DeriveJsonEncoder.gen[DbUserSessions]
    given JsonDecoder[DbUserSessions] = DeriveJsonDecoder.gen[DbUserSessions]
          
