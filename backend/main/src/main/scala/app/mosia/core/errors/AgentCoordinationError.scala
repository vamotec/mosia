package app.mosia.core.errors

case class AgentCoordinationError (message: String, cause: Throwable = null) extends Exception(message, cause)
