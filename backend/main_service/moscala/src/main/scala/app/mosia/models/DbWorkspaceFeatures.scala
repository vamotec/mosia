
// AUTO-GENERATED Quill data model
package app.mosia.models

import app.mosia.core.types.JSONValue
import io.getquill.*
import zio.json.*

case class DbWorkspaceFeatures(
  id: Int, 
  workspaceId: java.util.UUID, 
  featureId: Int, 
  name: String, 
  `type`: Int, 
  reason: String, 
  configs: JSONValue, 
  createdAt: java.time.Instant, 
  expiredAt: Option[java.time.Instant], 
  activated: Boolean
)

object DbWorkspaceFeatures:
    inline given schema: Quoted[EntityQuery[DbWorkspaceFeatures]] = quote:
      querySchema[DbWorkspaceFeatures]("public.workspace_features")

    given JsonEncoder[DbWorkspaceFeatures] = DeriveJsonEncoder.gen[DbWorkspaceFeatures]
    given JsonDecoder[DbWorkspaceFeatures] = DeriveJsonDecoder.gen[DbWorkspaceFeatures]
          
