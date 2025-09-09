package app.mosia.infra.dao

import app.mosia.domain.model.input.UserFeaturesInput
import app.mosia.models.*
import io.getquill.*
import zio.*

import java.util.UUID
import javax.sql.DataSource

trait UserFeaturesDao {
  def find(userId: UUID): RIO[DataSource, Option[DbUserFeatures]]
  def findType(userId: UUID, `type`: Int, activated: Boolean): RIO[DataSource, Option[DbUserFeatures]]
  def findName(userId: UUID, name: String): RIO[DataSource, Option[DbUserFeatures]]
  def count(userId: UUID, name: String): RIO[DataSource, Long]
  def list(userId: UUID): RIO[DataSource, List[DbUserFeatures]]
  def listType(userId: UUID, `type`: Int): RIO[DataSource, List[DbUserFeatures]]
  def create(data: UserFeaturesInput): RIO[DataSource, DbUserFeatures]
  def update(data: UserFeaturesInput): RIO[DataSource, Long]
}
