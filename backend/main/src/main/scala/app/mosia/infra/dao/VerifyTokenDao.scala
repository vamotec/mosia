package app.mosia.infra.dao

import app.mosia.domain.model.input.VerifyTokenInput
import app.mosia.models.DbVerifyTokens
import zio.{ RIO, Task }

import java.util.UUID
import javax.sql.DataSource

trait VerifyTokenDao:
  def create(data: VerifyTokenInput): RIO[DataSource, Long]

  def findUnique(token: UUID, `type`: Int): RIO[DataSource, Option[DbVerifyTokens]]

  def deleteMany(token: UUID, `type`: Int): RIO[DataSource, Long]

  def deleteExpired(): RIO[DataSource, Long]
