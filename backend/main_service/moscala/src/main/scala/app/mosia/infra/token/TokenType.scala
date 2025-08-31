package app.mosia.infra.token

enum TokenType:
  case SignIn, VerifyEmail, ChangeEmail, ChangePassword, Challenge
  def toInt: Int = this match
    case SignIn         => 0
    case VerifyEmail    => 1
    case ChangeEmail    => 2
    case ChangePassword => 3
    case Challenge      => 4

object TokenType:
  def fromInt(i: Int): Option[TokenType] = i match
    case 0 => Some(SignIn)
    case 1 => Some(VerifyEmail)
    case 2 => Some(ChangeEmail)
    case 3 => Some(ChangePassword)
    case 4 => Some(Challenge)
    case _ => None
