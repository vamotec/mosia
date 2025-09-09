package app.mosia.core.configs

import zio.Config.*
import zio.json.{ DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder }

case class MailerConfig(
  smtpHost: String,
  smtpPort: Int,
  username: String,
  password: String,
  senderName: Option[String] = Some("MosiaApp"),
  debug: Option[Boolean] = Some(false)
) derives JsonCodec
