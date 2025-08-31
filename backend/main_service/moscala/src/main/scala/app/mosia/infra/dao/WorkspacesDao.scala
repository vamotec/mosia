package app.mosia.infra.dao

import app.mosia.models.DbWorkspaces
import zio.RIO

import java.util.UUID
import javax.sql.DataSource

trait WorkspacesDao {
  def getById(id: UUID): RIO[DataSource, List[DbWorkspaces]]
}
