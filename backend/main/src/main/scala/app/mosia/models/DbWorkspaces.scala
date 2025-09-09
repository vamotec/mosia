
// AUTO-GENERATED Quill data model
package app.mosia.models

import app.mosia.core.types.JSONValue
import io.getquill.*
import zio.json.*

case class DbWorkspaces(
  id: java.util.UUID, 
  publicSpace: Boolean, 
  name: Option[String], 
  avatarKey: Option[String], 
  enableAi: Boolean, 
  enableUrlPreview: Boolean, 
  createdAt: java.time.Instant
)

object DbWorkspaces:
    inline given schema: Quoted[EntityQuery[DbWorkspaces]] = quote:
      querySchema[DbWorkspaces]("public.workspaces")

    given JsonEncoder[DbWorkspaces] = DeriveJsonEncoder.gen[DbWorkspaces]
    given JsonDecoder[DbWorkspaces] = DeriveJsonDecoder.gen[DbWorkspaces]
          
