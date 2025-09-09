package app.mosia.infra.mailer

import app.mosia.infra.mailer.Mailer.*
import zio.Task

trait MailerService {
  def sendCallback(mail: MailMessage, props: MailerProps): Task[Boolean]
  def send(mail: MailMessage): Task[Boolean]
}
