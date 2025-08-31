package app.mosia.domain.model

import app.mosia.domain.model.Id.UserSessionId
import zio.json.*

import java.time.OffsetDateTime
import java.util.UUID

case class UserSessions(
  id: Id.UserSessionId,
  sessionId: UUID,
  userId: UUID,
  createdAt: OffsetDateTime,
  lastAccessedAt: OffsetDateTime, // 添加最后访问时间
  expiresAt: OffsetDateTime,      // 改为必填，不使用Option
  isActive: Boolean = true        // 添加活跃状态
)
