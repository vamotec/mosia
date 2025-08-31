package app.mosia.core.errors

sealed trait MapperError                       extends Exception
case class InvalidUserIdError(message: String) extends MapperError
case class InvalidEmailError(message: String)  extends MapperError
