package app.mosia.domain.model

sealed trait SessionResult
object SessionResult:
  case class UserSessionWithUser(session: UserSessions, user: Users) extends SessionResult
  case class UserSessionWithOutUser(session: UserSessions)           extends SessionResult
