package app.mosia.infra.helpers.server

import app.mosia.core.configs.AppConfig
import app.mosia.core.types.JSONValue
import app.mosia.domain.model.input.ConfigInput
import app.mosia.infra.features.ServerFeature
import zio.json.ast.Json
import zio.*

import java.util.UUID
import javax.sql.DataSource

trait ServerHelper:
  def initialized: Task[Boolean]
  def getConfig: Task[AppConfig]
  def enableFeature(feature: ServerFeature): Task[Unit]
  def disableFeature(feature: ServerFeature): Task[Unit]
  def getFeatures: Task[List[ServerFeature]]
  def updateConfig(
    userId: UUID,
    updates: List[ConfigInput]
  ): Task[AppConfig]
  def onConfigChangedBroadcast(event: Map[String, String]): Task[Unit]
  def revalidateConfig: Task[Unit]
  def setup: Task[Unit]
  def loadDbOverrides: Task[Map[String, JSONValue]]
