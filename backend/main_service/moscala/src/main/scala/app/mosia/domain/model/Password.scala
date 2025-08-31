package app.mosia.domain.model

import app.mosia.core.errors.ErrorMessage
import org.mindrot.jbcrypt.BCrypt
import zio.json.*

// 密码类型，隐藏哈希实现细节
case class Password private (hash: String):
  def verify(plainPassword: String): Boolean =
    BCrypt.checkpw(plainPassword, hash)

object Password:
  // 工厂方法，防止直接构造无效密码
  def fromPlainText(plainPassword: String): Password =
    require(plainPassword.nonEmpty, "Password cannot be empty")
    Password(BCrypt.hashpw(plainPassword, BCrypt.gensalt()))

  def fromHash(hash: String): Password =
    require(hash.nonEmpty, "Password hash cannot be empty")
    Password(hash)

  given JsonEncoder[Password] = DeriveJsonEncoder.gen[Password]
  given JsonDecoder[Password] = DeriveJsonDecoder.gen[Password]
