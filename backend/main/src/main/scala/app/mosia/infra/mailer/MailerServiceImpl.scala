package app.mosia.infra.mailer

import app.mosia.core.configs.AppConfig
import app.mosia.infra.eventbus.EventBus
import app.mosia.infra.mailer.Mailer.*
import app.mosia.infra.mailer.Mailer.MailPriority.*
import app.mosia.infra.mailer.Mailer.MailTemplate.*
import org.simplejavamail.api.email.Email
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import scalatags.Text.all.*
import scalatags.Text.tags2.{ style, title }
import org.apache.kafka.clients.producer.ProducerRecord
import zio.kafka.producer.*
import zio.kafka.serde.*
import zio.*
import zio.json.*
import zio.kafka.consumer.*
import zio.stream.ZStream

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.util.matching.Regex

case class MailerServiceImpl(
  configRef: Ref[AppConfig],
  statsRef: Ref[MailStats],
  rateLimiter: Ref[Map[String, List[Instant]]],
  eventBus: EventBus
) extends MailerService:
  import MailerServiceImpl.*
  // 邮箱地址验证
  private val emailRegex: Regex =
    "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$".r

  private def validateEmail(email: String): Boolean =
    emailRegex.matches(email) && email.length <= 254

  // 速率限制检查 (每分钟最多50封邮件)
  private def checkRateLimit(email: String): Task[Boolean] =
    for
      now         <- Clock.instant
      limits      <- rateLimiter.get
      emailLimits  = limits.getOrElse(email, List.empty)
      recentLimits = emailLimits.filter(_.isAfter(now.minusSeconds(60)))
      _           <- rateLimiter.update(_.updated(email, recentLimits))
    yield recentLimits.length < 50

  // 创建邮件发送器（带连接池和重用）
  private def createMailer(config: AppConfig): Task[Mailer] =
    ZIO.attempt {
      MailerBuilder
        .withSMTPServer(
          config.mailer.smtpHost,
          config.mailer.smtpPort,
          config.mailer.username,
          config.mailer.password
        )
        .withTransportStrategy(TransportStrategy.SMTP_TLS)
        .withSessionTimeout(30 * 1000)                             // 30秒超时
        .withProperty("mail.smtp.connectionpoolsize", "10")
        .withProperty("mail.smtp.connectionpooltimeout", "300000") // 5分钟
        .withDebugLogging(config.mailer.debug.getOrElse(false))
        .buildMailer()
    }.mapError(err => new RuntimeException(s"Failed to create mailer: ${err.getMessage}"))

  // 生成HTML邮件内容
  private def generateEmailContent(template: MailTemplate, data: Map[String, String]): Task[String] =
    template match
      case WelcomeTemplate       =>
        ZIO.succeed(welcomeTemplate(data.getOrElse("email", ""), data.getOrElse("name", "用户")))
      case CallbackTemplate      =>
        ZIO.succeed(callbackTemplate(data.getOrElse("callbackUrl", ""), data.getOrElse("name", "用户")))
      case ResetPasswordTemplate =>
        ZIO.succeed(resetPasswordTemplate(data.getOrElse("resetUrl", ""), data.getOrElse("name", "用户")))
      case VerificationTemplate  =>
        ZIO.succeed(verificationTemplate(data.getOrElse("verifyUrl", ""), data.getOrElse("name", "用户")))

  // 发送邮件的核心逻辑
  private def sendEmailInternal(enhancedMail: EnhancedMail): Task[MailResult] =
    for
      // 验证邮箱格式
      _ <- ZIO
             .fail(new IllegalArgumentException("Invalid email format"))
             .when(!validateEmail(enhancedMail.to))

      // 检查速率限制
      canSend <- checkRateLimit(enhancedMail.to)
      _       <- ZIO
                   .fail(new RuntimeException("Rate limit exceeded"))
                   .when(!canSend)

      config      <- configRef.get
      mailer      <- createMailer(config)
      htmlContent <- generateEmailContent(enhancedMail.template, enhancedMail.templateData)

      email <- ZIO.attempt {
                 EmailBuilder
                   .startingBlank()
                   .from(config.mailer.senderName.getOrElse("System"), config.mailer.username)
                   .to(enhancedMail.to)
                   .withSubject(enhancedMail.subject)
                   .withHTMLText(htmlContent)
                   .withPlainText(htmlToPlainText(htmlContent)) // 提供纯文本版本
                   .withHeader("X-Priority", priorityToHeader(enhancedMail.priority))
                   .withHeader("X-Mailer", "MosiaApp-1.0")
                   .buildEmail()
               }

      result <- ZIO.attemptBlocking {
                  mailer.sendMail(email)
                  MailSuccess
                }
                  .timeoutFail(new RuntimeException("Email send timeout"))(Duration(30, TimeUnit.SECONDS))
                  .catchAll { err =>
                    val isRetryable = isRetryableError(err)
                    ZIO.succeed(MailFailure(err.getMessage, isRetryable))
                  }

      // 更新统计
      _ <- result match
             case MailSuccess                   =>
               statsRef.update(s => s.copy(sent = s.sent + 1)) *>
                 ZIO.logInfo(s"Email sent successfully to ${enhancedMail.to}")
             case MailFailure(error, retryable) =>
               statsRef.update(s => s.copy(failed = s.failed + 1)) *>
                 ZIO.logError(s"Failed to send email to ${enhancedMail.to}: $error") *>
                 (if (retryable && enhancedMail.retryCount < enhancedMail.maxRetries)
                    scheduleRetry(enhancedMail.copy(retryCount = enhancedMail.retryCount + 1))
                  else ZIO.unit)
    yield result

  // 判断错误是否可重试
  private def isRetryableError(error: Throwable): Boolean =
    error.getMessage match
      case msg if msg.contains("timeout")    => true
      case msg if msg.contains("connection") => true
      case msg if msg.contains("temporary")  => true
      case msg if msg.contains("rate limit") => true
      case _                                 => false

  // 安排重试 - 发送到Kafka重试Topic
  private def scheduleRetry(mail: EnhancedMail): Task[Unit] = {
    val retryTopic = s"mail-retry-${mail.retryCount}"             // 按重试次数分 Topic
    val delayMs    = (math.pow(2, mail.retryCount) * 1000).toLong // 指数退避

    for {
      _ <- statsRef.update(s => s.copy(retried = s.retried + 1))
      _ <- eventBus.emit(
             retryTopic,
             mail.copy(scheduledAt = Some(Instant.now().plusMillis(delayMs))) //  mail 中 scheduledAt 字段
           )
      _ <- ZIO.logInfo(s"Scheduled retry ${mail.retryCount} for ${mail.to} in ${delayMs}ms")
    } yield ()
  }

  // HTML转纯文本 (简单实现)
  private def htmlToPlainText(html: String): String =
    html
      .replaceAll("<[^>]+>", "")
      .replaceAll("\\s+", " ")
      .trim

  // 优先级转Header
  private def priorityToHeader(priority: MailPriority): String =
    priority match
      case High   => "1"
      case Normal => "3"
      case Low    => "5"

  // 公共接口实现 - 异步发送到Kafka
  // 高优先级邮件发送到专门的topic
  override def sendCallback(mail: MailMessage, props: MailerProps): Task[Boolean] =
    val enhancedMail = EnhancedMail(
      to = mail.to,
      subject = "账户更新通知",
      template = CallbackTemplate,
      templateData = Map(
        "callbackUrl" -> props.callbackUrl,
        "name"        -> mail.to.split("@").head
      ),
      priority = High // 回调邮件优先级高
    )
    eventBus.emit("mail-priority-queue", enhancedMail).as(true)

  override def send(mail: MailMessage): Task[Boolean] =
    val enhancedMail = EnhancedMail(
      to = mail.to,
      subject = "欢迎注册",
      template = WelcomeTemplate,
      templateData = Map(
        "email" -> mail.to,
        "name"  -> mail.to.split("@").head
      )
    )
    eventBus.emit("mail-queue", enhancedMail).as(true)

  // 批量发送 - 批量写入Kafka
  def sendBatch(mails: List[EnhancedMail]): Task[Unit] =
    ZIO
      .foreach(mails) { mail =>
        val topic = mail.priority match
          case High => "mail-priority-queue"
          case _    => "mail-queue"
        eventBus.emit(topic, mail) // 直接复用 emit
      }
      .unit

  // 同步发送邮件（用于立即需要结果的场景）
  def sendSync(mail: EnhancedMail): Task[MailResult] =
    sendEmailInternal(mail)

  // 获取发送统计
  def getStats: Task[MailStats] = statsRef.get

  // 清理过期的速率限制记录
  private def cleanupRateLimits: Task[Unit] =
    for
      now <- Clock.instant
      _   <- rateLimiter.update(
               _.view
                 .mapValues(
                   _.filter(_.isAfter(now.minusSeconds(300))) // 保留5分钟内的记录
                 )
                 .filter(_._2.nonEmpty)
                 .toMap
             )
    yield ()

object MailerServiceImpl:

  // Kafka Topics 定义
  private val MAIL_QUEUE_TOPIC          = "mail-queue"
  private val MAIL_PRIORITY_QUEUE_TOPIC = "mail-priority-queue"
  private val MAIL_RETRY_TOPIC_PREFIX   = "mail-retry-"
  private val MAIL_DLQ_TOPIC            = "mail-dlq" // 死信队列

  // 欢迎邮件模板
  private def welcomeTemplate(email: String, name: String): String =
    html(
      head(
        meta(charset      := "UTF-8"),
        meta(attr("name") := "viewport", content := "width=device-width, initial-scale=1.0"),
        title("欢迎注册"),
        style("""
          body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
            background-color: #f9f9f9;
          }
          .container {
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
          }
          .header {
            text-align: center;
            border-bottom: 2px solid #4CAF50;
            padding-bottom: 20px;
            margin-bottom: 30px;
          }
          .highlight {
            color: #4CAF50;
            font-weight: bold;
          }
          .features {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 5px;
            margin: 20px 0;
          }
          .footer {
            text-align: center;
            padding-top: 20px;
            border-top: 1px solid #eee;
            color: #666;
            font-size: 12px;
          }
        """)
      ),
      body(
        div(cls := "container")(
          div(cls := "header")(
            h1(s"欢迎 $name!")
          ),
          p(s"感谢您注册我们的服务！您的注册邮箱是："),
          p(cls := "highlight")(email),
          div(cls := "features")(
            h3("您现在可以体验以下功能："),
            ul(
              li("个性化设置和偏好管理"),
              li("实时通知和更新"),
              li("安全的数据同步"),
              li("24/7 客户支持")
            )
          ),
          if (email.endsWith("@example.com"))
            div(
              attr("style") := "background: #fff3cd; padding: 15px; border-radius: 5px; border-left: 4px solid #ffc107;"
            )(
              p(strong("注意："), "检测到测试账号，部分功能可能受限")
            )
          else
            div(
              attr("style") := "background: #d1ecf1; padding: 15px; border-radius: 5px; border-left: 4px solid #17a2b8;"
            )(
              p("请", strong("验证您的邮箱地址"), "以完全激活账户")
            ),
          div(cls := "footer")(
            p("此邮件由系统自动发送，请勿回复"),
            p("© 2025 MosiaApp. All rights reserved.")
          )
        )
      )
    ).render

  // 回调邮件模板
  private def callbackTemplate(callbackUrl: String, name: String): String =
    html(
      head(
        meta(charset      := "UTF-8"),
        meta(attr("name") := "viewport", content := "width=device-width, initial-scale=1.0"),
        title("账户更新通知"),
        style("""
          body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
            background-color: #f9f9f9;
          }
          .container {
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
          }
          .btn {
            display: inline-block;
            padding: 12px 24px;
            background-color: #007bff;
            color: white !important;
            text-decoration: none;
            border-radius: 5px;
            margin: 20px 0;
          }
          .btn:hover {
            background-color: #0056b3;
          }
        """)
      ),
      body(
        div(cls := "container")(
          h1(s"你好 $name,"),
          p("您的账户信息已成功更新。"),
          p("如果这不是您的操作，请立即联系我们的客服团队。"),
          a(cls := "btn", href := callbackUrl)("查看详情"),
          p("感谢您使用我们的服务！")
        )
      )
    ).render

  // 密码重置邮件模板
  private def resetPasswordTemplate(resetUrl: String, name: String): String =
    html(
      head(
        meta(charset := "UTF-8"),
        title("密码重置")
      ),
      body(
        h1(s"你好 $name,"),
        p("您请求重置密码。请点击下面的链接重置您的密码："),
        a(href := resetUrl)("重置密码"),
        p("如果您没有请求重置密码，请忽略此邮件。"),
        p("此链接将在2小时后过期。")
      )
    ).render

  // 邮箱验证模板
  private def verificationTemplate(verifyUrl: String, name: String): String =
    html(
      head(
        meta(charset := "UTF-8"),
        title("邮箱验证")
      ),
      body(
        h1(s"你好 $name,"),
        p("请点击下面的链接验证您的邮箱地址："),
        a(href := verifyUrl)("验证邮箱"),
        p("验证后您将能够使用所有功能。")
      )
    ).render

  // Kafka消费者 - 处理邮件队列
  private def startMailProcessor(service: MailerServiceImpl): UIO[Fiber.Runtime[Throwable, Unit]] =
    service.eventBus
      .subscribe[EnhancedMail](MAIL_QUEUE_TOPIC, MAIL_PRIORITY_QUEUE_TOPIC) // 返回 ZStream[Any, Throwable, EnhancedMail]
      .mapZIO { mail =>
        for {
          result <- service.sendEmailInternal(mail)
          _      <- result match
                      case MailSuccess =>
                        ZIO.logInfo(s"Successfully processed mail for ${mail.to}")

                      case MailFailure(error, retryable) =>
                        if (retryable && mail.retryCount < mail.maxRetries)
                          service.scheduleRetry(mail.copy(retryCount = mail.retryCount + 1))
                        else
                          sendToDeadLetterQueue(service, mail, error) *>
                            ZIO.logError(s"Mail failed permanently for ${mail.to}: $error")
        } yield ()
      }
      .runDrain
      .fork

  // 重试队列处理
  private def startRetryProcessor(
    service: MailerServiceImpl
  ): UIO[List[Fiber.Runtime[Throwable, Unit]]] = {

    val retryTopics = (1 to 3).map(i => s"$MAIL_RETRY_TOPIC_PREFIX$i")

    ZIO.foreach(retryTopics.toList) { topic =>
      // 使用 EventBus 订阅 retry topic
      service.eventBus
        .subscribe[EnhancedMail](topic)
        .mapZIO { mail =>
          for {
            now          <- Clock.instant
            shouldProcess = mail.scheduledAt.get.isBefore(now) || mail.scheduledAt.equals(now) // scheduledAt 字段控制延迟

            _ <- if (shouldProcess)
                   service.sendEmailInternal(mail).flatMap {
                     case MailSuccess =>
                       ZIO.logInfo(s"Retry successful for ${mail.to}")

                     case MailFailure(error, retryable) =>
                       if (retryable && mail.retryCount < mail.maxRetries)
                         service.scheduleRetry(mail.copy(retryCount = mail.retryCount + 1))
                       else
                         sendToDeadLetterQueue(service, mail, error)
                   }
                 else
                   // 消息还没到重试时间，延迟 1 秒后重新放回队列
                   ZIO.sleep(1.second) *> service.eventBus.emit(topic, mail)
          } yield ()
        }
        .runDrain
        .fork
    }
  }

  // 发送到死信队列
  private def sendToDeadLetterQueue(service: MailerServiceImpl, mail: EnhancedMail, error: String): Task[Unit] =
    val dlqMail = mail.copy(templateData = mail.templateData + ("error" -> error))
    service.eventBus.emit(MAIL_DLQ_TOPIC, dlqMail)

  // 定期清理任务
  private def startCleanupWorker(service: MailerServiceImpl): Task[Fiber.Runtime[Throwable, Nothing]] =
    service.cleanupRateLimits
      .repeat(Schedule.fixed(Duration(5, TimeUnit.MINUTES)))
      .forever
      .fork

  def make: ZIO[EventBus & Ref[AppConfig], Throwable, MailerService] =
    for
      configRef   <- ZIO.service[Ref[AppConfig]] // 需要注入AppConfig
      statsRef    <- Ref.make(MailStats())
      rateLimiter <- Ref.make(Map.empty[String, List[Instant]])
      eventBus    <- ZIO.service[EventBus]       // 直接从环境获取
      service      = new MailerServiceImpl(configRef, statsRef, rateLimiter, eventBus)
      // 启动后台处理任务
      _           <- startMailProcessor(service)
      _           <- startRetryProcessor(service)
      _           <- startCleanupWorker(service)
    yield service

  val live: ZLayer[EventBus & Ref[AppConfig], Throwable, MailerService] = ZLayer.fromZIO(make)

end MailerServiceImpl
