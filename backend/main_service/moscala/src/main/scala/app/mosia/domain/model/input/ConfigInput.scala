package app.mosia.domain.model.input

import app.mosia.core.types.JSON
import app.mosia.domain.model.update.ConfigUpdate
import app.mosia.infra.helpers.server.ConfigDescriptorEntry.APP_CONFIG_DESCRIPTORS

final case class ConfigInput(key: String, module: String, value: JSON):
  def validate(): List[String] =
    APP_CONFIG_DESCRIPTORS
      .get(module)
      .flatMap(_.get(key))
      .map(_.validate(value.value))
      .map {
        case Left(error) => error.msgs
        case Right(_)    => Nil
      }
      .getOrElse(List(s"Unknown config: $module.$key"))
  def toDbFormat: ConfigUpdate = ConfigUpdate(s"$module.$key", value.value)
