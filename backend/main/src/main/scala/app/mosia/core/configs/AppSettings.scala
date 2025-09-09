package app.mosia.core.configs

import zio.json.JsonCodec

case class AppSettings(
  name: Option[String] = Some("MosiaApp"),
  baseUrl: String = "https://mosia.app",
  supportEmail: Option[String] = Some("support@mosia.app")
) derives JsonCodec
