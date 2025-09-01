
// AUTO-GENERATED Quill data model
package app.mosia.models

import app.mosia.core.types.JSONValue
import io.getquill.*
import zio.json.*

case class DbVerifyTokens(
  token: java.util.UUID, 
  `type`: Int, 
  credential: Option[String], 
  expiresAt: java.time.Instant
)

object DbVerifyTokens:
    inline given schema: Quoted[EntityQuery[DbVerifyTokens]] = quote:
      querySchema[DbVerifyTokens]("public.verify_tokens")

    given JsonEncoder[DbVerifyTokens] = DeriveJsonEncoder.gen[DbVerifyTokens]
    given JsonDecoder[DbVerifyTokens] = DeriveJsonDecoder.gen[DbVerifyTokens]
          
