package app.mosia.infra.token

import zio.Task

trait TokenService:
  def generatePasswordResetToken(email: String): Task[String]

  def generateSetPasswordToken(email: String, userId: String): Task[String]

  def generateEmailVerificationToken(email: String, userId: String): Task[String]

  def validatePasswordResetToken(token: String): Task[Option[String]]

  def validateSetPasswordToken(token: String): Task[Option[(String, String)]]

  def validateEmailVerificationToken(token: String): Task[Option[(String, String)]]
