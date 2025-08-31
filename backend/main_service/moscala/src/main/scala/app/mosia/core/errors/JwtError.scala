package app.mosia.core.errors

sealed trait JwtError                           extends Exception
case class InvalidTokenError(message: String)   extends JwtError
case class ExpiredTokenError(message: String)   extends JwtError
case class MalformedTokenError(message: String) extends JwtError
