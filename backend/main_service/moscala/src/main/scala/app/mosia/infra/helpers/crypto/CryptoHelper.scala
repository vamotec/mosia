package app.mosia.infra.helpers.crypto

import app.mosia.infra.helpers.crypto.CryptoUtils.NONCE_LENGTH
import zio.*

trait CryptoHelper {
  def init(): Task[Unit]
  def sign(data: String): Task[String]
  def verify(signatureWithData: String): Task[Boolean]
  def encrypt(data: String): Task[String]
  def decrypt(encrypted: String): Task[String]
  def compare(lhs: String, rhs: String): Task[Boolean]
  def randomBytes(length: Int = NONCE_LENGTH): Task[Array[Byte]]
//  def randomInt(min: Int, max: Int): Task[Int]
//  def otp(length: Int = 6): Task[String]
  def sha256(data: String): Task[Array[Byte]]
  def registerListeners(): UIO[Unit]
  def isValidEmail(email: String): Task[Boolean]
  def isValidPassword(password: String): Task[Boolean]
}
