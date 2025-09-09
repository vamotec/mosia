package app.mosia.infra.repository.impl

import app.mosia.domain.model.Configs
import app.mosia.domain.model.update.ConfigUpdate
import app.mosia.infra.dao.DaoModule
import app.mosia.infra.dao.impl.DaoModuleImpl
import app.mosia.infra.repository.ConfigsRepo
import app.mosia.mapper.DomainMappers.*
import zio.*

import java.util.UUID
import javax.sql.DataSource

private case class ConfigsRepoImpl(dao: DaoModule, ds: DataSource) extends ConfigsRepo:
  override def load(): Task[List[Configs]] =
    for
      db     <- dao.configsDao.findAll().provideEnvironment(ZEnvironment(ds))
      result <- toDomainList(db)
    yield result

  override def save(userId: UUID, updates: List[ConfigUpdate]): Task[List[Configs]] =
    ZIO.foreachPar(updates): update =>
      for
        db     <- dao.configsDao.upsert(update.key, update.value, userId).provideEnvironment(ZEnvironment(ds))
        result <- toDomain(db)
      yield result

object ConfigsRepoImpl:
  private def make: ZIO[DaoModule with DataSource, Nothing, ConfigsRepo] =
    for
      dao <- ZIO.service[DaoModule]
      ds  <- ZIO.service[DataSource]
    yield new ConfigsRepoImpl(dao, ds)

  val live: ZLayer[DataSource, Throwable, ConfigsRepo] = DaoModuleImpl.layer >>> ZLayer.fromZIO(make)
