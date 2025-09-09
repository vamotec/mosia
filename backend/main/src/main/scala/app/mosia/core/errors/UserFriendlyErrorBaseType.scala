package app.mosia.core.errors

import zio.http.Status

enum UserFriendlyErrorBaseType:
  case InternalServerError
  case ResourceNotFound
  case TooManyRequests
  case Unauthorized
  case BadRequest
  case InvalidInput
  case ResourceAlreadyExists
  case NetworkError
  case ActionForbidden
  case NoPermission
  case AuthenticationRequired
  case QuotaExceeded

  def toStatusCode: Int = this match
    case InternalServerError    => Status.InternalServerError.code
    case ResourceNotFound       => Status.NotFound.code
    case TooManyRequests        => Status.TooManyRequests.code
    case Unauthorized           => Status.Unauthorized.code
    case BadRequest             => Status.BadRequest.code
    case InvalidInput           => Status.BadRequest.code
    case ResourceAlreadyExists  => Status.BadRequest.code
    case NetworkError           => Status.GatewayTimeout.code
    case ActionForbidden        => Status.Forbidden.code
    case NoPermission           => Status.Forbidden.code
    case AuthenticationRequired => Status.Unauthorized.code
    case QuotaExceeded          => Status.PaymentRequired.code

  def toStringCode: String = this match
    case InternalServerError    => "INTERNAL_SERVER_ERROR"
    case ResourceNotFound       => "RESOURCE_NOT_FOUND"
    case TooManyRequests        => "TOO_MANY_REQUESTS"
    case Unauthorized           => "UNAUTHORIZED"
    case BadRequest             => "BAD_REQUEST"
    case InvalidInput           => "INVALID_INPUT"
    case ResourceAlreadyExists  => "RESOURCE_ALREADY_EXISTS"
    case NetworkError           => "NETWORK_ERROR"
    case ActionForbidden        => "ACTION_FORBIDDEN"
    case NoPermission           => "NO_PERMISSION"
    case AuthenticationRequired => "AUTHENTICATION_REQUIRED"
    case QuotaExceeded          => "QUOTA_EXCEEDED"
