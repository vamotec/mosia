package app.mosia.infra.mailer

import zio.json.*

import java.time.Instant

object Mailer:
  case class MailerProps(callbackUrl: String)
  // 邮件发送结果
  sealed trait MailResult
  case object MailSuccess                                   extends MailResult
  case class MailFailure(error: String, retryable: Boolean) extends MailResult

  // Kafka队列中使用的邮件消息格式
  case class MailMessage(
    id: String,                                    // 邮件唯一ID
    to: String,                                    // 收件人邮箱
    subject: String,                               // 邮件主题
    content: String,                               // 邮件内容（可以是HTML或纯文本）
    priority: String = "normal",                   // 优先级: "high", "normal", "low"
    retryCount: Int = 0,                           // 当前重试次数
    maxRetries: Int = 3,                           // 最大重试次数
    createdAt: Instant = Instant.now(),            // 创建时间
    scheduledAt: Option[Instant] = None,           // 定时发送时间
    // 可选字段
    from: Option[String] = None,                   // 发件人（如果不同于默认）
    fromName: Option[String] = None,               // 发件人姓名
    replyTo: Option[String] = None,                // 回复邮箱
    // 邮件类型和模板信息
    mailType: String = "generic",                  // 邮件类型标识
    templateName: Option[String] = None,           // 模板名称
    templateData: Map[String, String] = Map.empty, // 模板数据
    // 元数据
    correlationId: Option[String] = None,          // 关联ID（用于追踪）
    userId: Option[String] = None,                 // 关联用户ID
    campaignId: Option[String] = None,             // 活动ID（用于营销邮件）
    // 发送状态跟踪
    attempts: List[MailAttempt] = List.empty,      // 发送尝试记录
    lastError: Option[String] = None               // 最后一次错误信息
  ) derives JsonCodec

  // 邮件发送尝试记录
  case class MailAttempt(
    attemptAt: Instant,                  // 尝试时间
    success: Boolean,                    // 是否成功
    error: Option[String] = None,        // 错误信息
    smtpResponse: Option[String] = None, // SMTP服务器响应
    processingTimeMs: Long = 0           // 处理时间（毫秒）
  ) derives JsonCodec

  // 邮件模板类型
  trait MailTemplate
  case object WelcomeTemplate       extends MailTemplate
  case object CallbackTemplate      extends MailTemplate
  case object ResetPasswordTemplate extends MailTemplate
  case object VerificationTemplate  extends MailTemplate

  sealed trait AuthMailTemplate          extends MailTemplate
  case object ChangePasswordTemplate     extends AuthMailTemplate
  case object SetPasswordTemplate        extends AuthMailTemplate
  case object VerifyChangeEmailTemplate  extends AuthMailTemplate
  case object SignInNotificationTemplate extends AuthMailTemplate
  case object SignInCodeTemplate         extends AuthMailTemplate

  object MailTemplate:
    // 方案 1: 统一的字符串映射（推荐）
    given JsonEncoder[MailTemplate] = JsonEncoder[String].contramap {
      // 基础模板
      case WelcomeTemplate       => "welcome"
      case CallbackTemplate      => "callback"
      case ResetPasswordTemplate => "reset_password"
      case VerificationTemplate  => "verification"

      // 认证模板
      case ChangePasswordTemplate     => "change_password"
      case SetPasswordTemplate        => "set_password"
      case VerifyChangeEmailTemplate  => "verify_change_email"
      case SignInNotificationTemplate => "sign_in_notification"
      case SignInCodeTemplate         => "sign_in_code"
    }

    given JsonDecoder[MailTemplate] = JsonDecoder[String].mapOrFail {
      // 基础模板
      case "welcome"        => Right(WelcomeTemplate)
      case "callback"       => Right(CallbackTemplate)
      case "reset_password" => Right(ResetPasswordTemplate)
      case "verification"   => Right(VerificationTemplate)

      // 认证模板
      case "change_password"      => Right(ChangePasswordTemplate)
      case "set_password"         => Right(SetPasswordTemplate)
      case "verify_change_email"  => Right(VerifyChangeEmailTemplate)
      case "sign_in_notification" => Right(SignInNotificationTemplate)
      case "sign_in_code"         => Right(SignInCodeTemplate)

      case other => Left(s"Unknown mail template: $other")
    }

  // 邮件优先级
  sealed trait MailPriority
  case object High   extends MailPriority
  case object Normal extends MailPriority
  case object Low    extends MailPriority

  object MailPriority:
    // 方案 1: 使用数字表示优先级（推荐）
    given JsonEncoder[MailPriority] = JsonEncoder[Int].contramap {
      case High   => 3
      case Normal => 2
      case Low    => 1
    }

    given JsonDecoder[MailPriority] = JsonDecoder[Int].mapOrFail {
      case 3     => Right(High)
      case 2     => Right(Normal)
      case 1     => Right(Low)
      case other => Left(s"Unknown mail priority: $other, expected 1-3")
    }

  // 增强的邮件数据结构
  case class EnhancedMail(
    to: String,
    subject: String,
    template: MailTemplate,
    templateData: Map[String, String] = Map.empty,
    priority: MailPriority = Normal,
    retryCount: Int = 0,
    maxRetries: Int = 3,
    scheduledAt: Option[Instant] = None,
    createdAt: Instant = Instant.now()
  ) derives JsonCodec

  // 邮件发送统计
  case class MailStats(
    sent: Long = 0,
    failed: Long = 0,
    retried: Long = 0,
    rateLimited: Long = 0
  )
