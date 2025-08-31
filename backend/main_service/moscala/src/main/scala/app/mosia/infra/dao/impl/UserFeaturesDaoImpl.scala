package app.mosia.infra.dao.impl

import app.mosia.domain.model.input.UserFeaturesInput
import app.mosia.infra.dao.impl.UserFeaturesDaoImpl.live
import app.mosia.infra.dao.{ UserFeaturesDao, WorkspacesDao }
import app.mosia.models.{ DbFeatures, DbUserFeatures }
import io.getquill.*
import zio.{ RIO, Task, URLayer, ZIO, ZLayer }

import java.util.UUID
import javax.sql.DataSource

case class UserFeaturesDaoImpl(dataSource: DataSource) extends UserFeaturesDao:
  import QuillContext.*

  override def find(userId: UUID): RIO[DataSource, Option[DbUserFeatures]] =
    inline def queries = quote:
      DbUserFeatures.schema.filter { uf =>
        uf.userId == lift(userId) &&
        uf.activated == lift(true)
      }

    run(queries).map(_.headOption)

  override def findType(userId: UUID, `type`: Int, activated: Boolean): RIO[DataSource, Option[DbUserFeatures]] =
    inline def queries = quote:
      DbUserFeatures.schema
        .filter(_.userId == lift(userId))
        .filter(_.`type` == lift(`type`))
        .filter(_.activated == lift(activated))

    run(queries).map(_.headOption)

  override def findName(userId: UUID, name: String): RIO[DataSource, Option[DbUserFeatures]] =
    inline def queries = quote:
      DbUserFeatures.schema
        .filter(_.userId == lift(userId))
        .filter(_.name == lift(name))
        .filter(_.activated == lift(true))

    run(queries).map(_.headOption)

  override def count(userId: UUID, name: String): RIO[DataSource, Long] =
    inline def queries = quote:
      DbUserFeatures.schema
        .filter(row => row.userId == lift(userId) && row.name == lift(name) && row.activated == lift(true))
        .size

    run(queries)

  override def list(userId: UUID): RIO[DataSource, List[DbUserFeatures]] =
    inline def queries = quote:
      DbUserFeatures.schema
        .filter(_.userId == lift(userId))
        .filter(_.activated == lift(true))

    run(queries)

  override def listType(userId: UUID, `type`: Int): RIO[DataSource, List[DbUserFeatures]] =
    inline def queries = quote:
      DbUserFeatures.schema
        .filter(_.userId == lift(userId))
        .filter(_.activated == lift(true))
        .filter(_.`type` == lift(`type`))

    run(queries)

  override def create(data: UserFeaturesInput): RIO[DataSource, DbUserFeatures] =
    inline def queries = quote:
      DbUserFeatures.schema
        .insert(
          _.userId    -> lift(data.userId),
          _.featureId -> lift(data.featureId),
          _.reason    -> lift(data.reason),
          _.activated -> lift(data.activated),
          _.name      -> lift(data.name),
          _.`type`    -> lift(data.`type`),
          _.expiredAt -> lift(data.expiredAt)
        )
        .returning(r => r)

    run(queries)

  override def update(data: UserFeaturesInput): RIO[DataSource, Long] =
    inline def queries = quote:
      DbUserFeatures.schema
        .filter(_.userId == lift(data.userId))
        .update(
          _.featureId -> lift(data.featureId),
          _.reason    -> lift(data.reason),
          _.activated -> lift(data.activated),
          _.name      -> lift(data.name),
          _.`type`    -> lift(data.`type`),
          _.expiredAt -> lift(data.expiredAt)
        )

    run(queries)

object UserFeaturesDaoImpl:
  val live: URLayer[DataSource, UserFeaturesDao] = ZLayer.fromFunction(UserFeaturesDaoImpl.apply _)
