package app.mosia.infra.repository

import app.mosia.domain.model.*
import app.mosia.domain.model.update.ConfigUpdate
import zio.*

import java.util.UUID
import javax.sql.DataSource

trait ConfigsRepo:
  def load(): Task[List[Configs]]
  def save(user: UUID, updates: List[ConfigUpdate]): Task[List[Configs]]
