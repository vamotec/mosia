package app.mosia.infra.auth

import app.mosia.infra.mailer.Mailer.*

object AuthMailTemplates {
  // 获取所有认证模板
  val allAuthTemplates: List[AuthMailTemplate] = List(
    ChangePasswordTemplate,
    SetPasswordTemplate,
    VerifyChangeEmailTemplate,
    SignInNotificationTemplate,
    SignInCodeTemplate
  )

  // 帮助方法：判断是否为认证模板
  def isAuthTemplate(template: MailTemplate): Boolean                   = template match {
    case _: AuthMailTemplate => true
    case _                   => false
  }
  // 密码重置模板
  def changePasswordTemplate(templateData: Map[String, String]): String = {
    val name        = templateData.getOrElse("name", "用户")
    val resetUrl    = templateData.getOrElse("resetUrl", "#")
    val expireHours = templateData.getOrElse("expireHours", "24")

    s"""
    <html>
      <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
        <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
          <h1 style="color: #dc3545; margin: 0;">密码重置请求</h1>
        </div>
        
        <p>你好 <strong>$name</strong>，</p>
        
        <p>我们收到了您的密码重置请求。请点击下面的按钮来重置您的密码：</p>
        
        <div style="text-align: center; margin: 30px 0;">
          <a href="$resetUrl" 
             style="background: #007bff; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block;">
            重置密码
          </a>
        </div>
        
        <p><strong>重要提醒：</strong></p>
        <ul>
          <li>此链接将在 $expireHours 小时后过期</li>
          <li>如果您没有请求重置密码，请忽略此邮件</li>
          <li>为了账户安全，请不要将此链接分享给他人</li>
        </ul>
        
        <div style="margin-top: 40px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px;">
          <p>此邮件由系统自动发送，请勿回复。</p>
        </div>
      </body>
    </html>
    """
  }

  // 登录验证码模板
  def signInCodeTemplate(templateData: Map[String, String]): String = {
    val name          = templateData.getOrElse("name", "用户")
    val code          = templateData.getOrElse("code", "000000")
    val expireMinutes = templateData.getOrElse("expireMinutes", "10")
    val ipAddress     = templateData.getOrElse("ipAddress", "未知")
    val location      = templateData.getOrElse("location", "未知位置")

    s"""
    <html>
      <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
        <div style="background: #e3f2fd; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
          <h1 style="color: #1976d2; margin: 0;">登录验证码</h1>
        </div>
        
        <p>你好 <strong>$name</strong>，</p>
        
        <p>您正在尝试登录账户，请使用以下验证码完成登录：</p>
        
        <div style="text-align: center; margin: 30px 0;">
          <div style="background: #f5f5f5; border: 2px dashed #ccc; padding: 20px; border-radius: 8px;">
            <span style="font-size: 32px; font-weight: bold; color: #1976d2; letter-spacing: 4px;">$code</span>
          </div>
        </div>
        
        <p><strong>登录信息：</strong></p>
        <ul>
          <li>IP地址: $ipAddress</li>
          <li>位置: $location</li>
          <li>验证码有效期: $expireMinutes 分钟</li>
        </ul>
        
        <div style="background: #fff3cd; padding: 15px; border-radius: 5px; border-left: 4px solid #ffc107;">
          <strong>安全提醒：</strong> 如果这不是您的操作，请立即更改密码并联系客服。
        </div>
        
        <div style="margin-top: 40px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px;">
          <p>此邮件由系统自动发送，请勿回复。验证码请勿分享给他人。</p>
        </div>
      </body>
    </html>
    """
  }
}
