package app.mosia.infra.dao.impl

import app.mosia.infra.dao.WorkspacesDao
import app.mosia.models.DbWorkspaces
import io.getquill.*
import zio.{ RIO, URLayer, ZIO, ZLayer }

import java.util.UUID
import javax.sql.DataSource

case class WorkspacesDaoImpl(dataSource: DataSource) extends WorkspacesDao {
  import QuillContext.*

  override def getById(id: UUID): RIO[DataSource, List[DbWorkspaces]] = {
    inline def queries = quote {
      DbWorkspaces.schema.filter(_.id == lift(id))
    }
    run(queries)
  }
}

object WorkspacesDaoImpl {
  val live: URLayer[DataSource, WorkspacesDao] = ZLayer.fromFunction(WorkspacesDaoImpl.apply _)
}
