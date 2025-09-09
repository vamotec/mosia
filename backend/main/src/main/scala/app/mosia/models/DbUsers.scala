
// AUTO-GENERATED Quill data model
package app.mosia.models

import app.mosia.core.types.JSONValue
import io.getquill.*
import zio.json.*

case class DbUsers(
  id: java.util.UUID, 
  email: String, 
  registered: Boolean, 
  disabled: Boolean, 
  name: String, 
  passwordHash: Option[String], 
  avatarUrl: Option[String], 
  emailVerified: Option[java.time.Instant], 
  createdAt: java.time.Instant, 
  updatedAt: java.time.Instant
)

object DbUsers:
    inline given schema: Quoted[EntityQuery[DbUsers]] = quote:
      querySchema[DbUsers]("public.users")

    given JsonEncoder[DbUsers] = DeriveJsonEncoder.gen[DbUsers]
    given JsonDecoder[DbUsers] = DeriveJsonDecoder.gen[DbUsers]
          
