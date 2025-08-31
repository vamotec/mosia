package app.mosia.infra.helpers.server

import app.mosia.core.configs.AppConfig
import app.mosia.core.errors.UserFriendlyError.*
import app.mosia.core.types.JSONValue
import app.mosia.domain.model.input.ConfigInput
import app.mosia.infra.eventbus.EventBus
import app.mosia.infra.features.ServerFeature
import app.mosia.infra.repository.RepoModule
import app.mosia.infra.repository.impl.RepoModuleImpl
import zio.{ IO, RIO, Ref, Task, ZIO, ZLayer }

import java.util.UUID
import javax.sql.DataSource

case class ServerHelperImpl(
  repo: RepoModule,
  configRef: Ref[AppConfig],
  features: Ref[Set[ServerFeature]],
  eventBus: EventBus
) extends ServerHelper:

  import ServerHelperImpl.*

  override def initialized: Task[Boolean] =
    for userCount <- repo.usersRepo.count()
    yield userCount > 0

  override def getConfig: Task[AppConfig] = configRef.get

  override def enableFeature(feature: ServerFeature): Task[Unit] =
    features.update(_ + feature)

  override def disableFeature(feature: ServerFeature): Task[Unit] =
    features.update(_ - feature)

  override def getFeatures: Task[List[ServerFeature]] =
    features.get.map(_.toList)

  override def updateConfig(
    userId: UUID,
    updates: List[ConfigInput]
  ): Task[AppConfig] =
    for
      _        <- validateConfig(updates)
      saved    <- repo.configsRepo.save(userId, updates.map(_.toDbFormat))
      overrides = saved.map(config => config.id -> config.config.json).toMap
      _        <- configRef.update(current => applyOverrides(current, overrides))
      _        <- eventBus.emit("config.changed", overrides)
      _        <- eventBus.broadcast("config.changed.broadcast", overrides)
      updated  <- configRef.get
    yield updated

  override def onConfigChangedBroadcast(event: Map[String, String]): Task[Unit] =
    val jsonMap: Map[String, JSONValue] = event.view.mapValues(JSONValue.JSONString(_)).toMap
    configRef.update(current => applyOverrides(current, jsonMap)) *>
      eventBus.emit("config.changed", event)

  override def revalidateConfig: Task[Unit] =
    for
      overrides <- loadDbOverrides
      _         <- configRef.update(current => applyOverrides(current, overrides))
      _         <- eventBus.emit("config.changed", overrides)
    yield ()

  override def setup: Task[Unit] =
    for
      overrides <- loadDbOverrides
      _         <- configRef.update(current => applyOverrides(current, overrides))
      current   <- configRef.get
      _         <- eventBus.emit("config.init", current)
    yield ()

  override def loadDbOverrides: Task[Map[String, JSONValue]] =
    repo.configsRepo.load().map(_.map(c => c.id -> c.config.json).toMap)

object ServerHelperImpl:
  private def validateConfig(updates: List[ConfigInput]): Task[Unit] =
    val errors = updates.flatMap(_.validate())
    if (errors.isEmpty) ZIO.unit
    else ZIO.fail(InvalidAppConfig(errors.mkString("\n")))

  private def applyOverrides(base: AppConfig, updates: Map[String, JSONValue]): AppConfig =
    overrideUtil(base, updates)

  private def make: ZIO[Ref[AppConfig] & EventBus & RepoModule, Nothing, ServerHelper] =
    for
      repo        <- ZIO.service[RepoModule]
      eventBus    <- ZIO.service[EventBus]
      configRef   <- ZIO.service[Ref[AppConfig]]
      featuresRef <- Ref.make(Set.empty[ServerFeature])
    yield new ServerHelperImpl(
      repo = repo,
      configRef = configRef,
      features = featuresRef,
      eventBus = eventBus
    )

  val live: ZLayer[DataSource & Ref[AppConfig] & EventBus, Throwable, ServerHelper] =
    RepoModuleImpl.layer >>> ZLayer.fromZIO(make)
