package app.mosia.application.dto

import app.mosia.domain.model.{ CurrentUser, Id, Users }
import app.mosia.domain.model.Email
import zio.json.*
import zio.*

import java.util.UUID

// API 响应中的用户信息
case class UserResponseDto(
  id: String,
  email: String,
  name: String,
  avatarUrl: Option[String],
  emailVerified: Boolean,
  createdAt: String
) derives JsonCodec:

  def toCurrentUser(sessionId: Option[String]): Task[CurrentUser] =
    ZIO.attempt {
      CurrentUser(
        id = Id[Users](UUID.fromString(id)),
        email = Email(email),
        name = name,
        avatarUrl = avatarUrl,
        emailVerified = emailVerified,
        sessionId = sessionId
      )
    }
