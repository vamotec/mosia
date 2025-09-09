
// AUTO-GENERATED Quill data model
package app.mosia.models

import app.mosia.core.types.JSONValue
import io.getquill.*
import zio.json.*

case class DbUserConnectedAccounts(
  id: java.util.UUID, 
  userId: java.util.UUID, 
  provider: String, 
  providerAccountId: String, 
  scope: Option[String], 
  accessToken: Option[String], 
  refreshToken: Option[String], 
  expiresAt: Option[java.time.Instant], 
  createdAt: java.time.Instant, 
  updatedAt: java.time.Instant
)

object DbUserConnectedAccounts:
    inline given schema: Quoted[EntityQuery[DbUserConnectedAccounts]] = quote:
      querySchema[DbUserConnectedAccounts]("public.user_connected_accounts")

    given JsonEncoder[DbUserConnectedAccounts] = DeriveJsonEncoder.gen[DbUserConnectedAccounts]
    given JsonDecoder[DbUserConnectedAccounts] = DeriveJsonDecoder.gen[DbUserConnectedAccounts]
          
