package app.mosia.infra.helpers

import app.mosia.core.configs.AppConfig
import org.flywaydb.core.Flyway
import zio.*

object FlywayMigration:
  def migrateZIO(config: AppConfig): Task[Unit] =
    ZIO.attempt {
      val db     = config.database
      val flyway = Flyway
        .configure()
        .dataSource(db.jdbcUrl, db.username, db.password)
        .locations("classpath:db/migration")
        .load()
      flyway.migrate()
      ()
    }
