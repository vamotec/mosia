package app.mosia.domain.model

import app.mosia.domain.model.Id.AccountId
import zio.json.*

case class Accounts(
  id: AccountId,
  userId: java.util.UUID,
  provider: String,
  providerAccountId: String,
  scope: Option[String],
  accessToken: Option[String],
  refreshToken: Option[String],
  expiresAt: Option[java.time.Instant]
)
