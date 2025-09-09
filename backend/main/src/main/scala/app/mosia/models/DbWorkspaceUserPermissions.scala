
// AUTO-GENERATED Quill data model
package app.mosia.models

import app.mosia.core.types.JSONValue
import io.getquill.*
import zio.json.*

case class DbWorkspaceUserPermissions(
  id: java.util.UUID, 
  workspaceId: java.util.UUID, 
  userId: java.util.UUID, 
  inviterId: Option[java.util.UUID], 
  `type`: Int, 
  status: String, 
  source: String, 
  accepted: Boolean, 
  updatedAt: java.time.Instant, 
  createdAt: java.time.Instant
)

object DbWorkspaceUserPermissions:
    inline given schema: Quoted[EntityQuery[DbWorkspaceUserPermissions]] = quote:
      querySchema[DbWorkspaceUserPermissions]("public.workspace_user_permissions")

    given JsonEncoder[DbWorkspaceUserPermissions] = DeriveJsonEncoder.gen[DbWorkspaceUserPermissions]
    given JsonDecoder[DbWorkspaceUserPermissions] = DeriveJsonDecoder.gen[DbWorkspaceUserPermissions]
          
