package app.mosia.core.errors

trait CryptoError extends Exception {
  def message: String
  def cause: Option[Throwable]
}

case class KeyGenerationError(message: String, cause: Option[Throwable] = None)    extends CryptoError
case class KeyConversionError(message: String, cause: Option[Throwable] = None)    extends CryptoError
case class InvalidKeyFormatError(message: String, cause: Option[Throwable] = None) extends CryptoError
