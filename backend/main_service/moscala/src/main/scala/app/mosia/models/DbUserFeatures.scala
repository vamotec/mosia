
// AUTO-GENERATED Quill data model
package app.mosia.models

import app.mosia.core.types.JSONValue
import io.getquill.*
import zio.json.*

case class DbUserFeatures(
  id: Int, 
  userId: java.util.UUID, 
  featureId: Int, 
  name: String, 
  `type`: Int, 
  reason: String, 
  createdAt: java.time.Instant, 
  expiredAt: Option[java.time.Instant], 
  activated: Boolean
)

object DbUserFeatures:
    inline given schema: Quoted[EntityQuery[DbUserFeatures]] = quote:
      querySchema[DbUserFeatures]("public.user_features")

    given JsonEncoder[DbUserFeatures] = DeriveJsonEncoder.gen[DbUserFeatures]
    given JsonDecoder[DbUserFeatures] = DeriveJsonDecoder.gen[DbUserFeatures]
          
