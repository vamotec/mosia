package app.mosia.core.errors

import jdk.internal.icu.impl.Utility.escape

sealed trait UserFriendlyError extends Throwable {
  def baseType: UserFriendlyErrorBaseType
  def name: ErrorNames
  def message: String
  def data: Option[Any]
  def requestId: Option[String]

  private def status: Int    = baseType.toStatusCode
  private def `type`: String = baseType.toStringCode
  def code: String           = toStringCode(name)

  override def getMessage: String = message

  private def toStringCode(e: ErrorNames): String = e.toString.toUpperCase
  def toText: String                              =
    s"""Status: $status
       |Type: ${`type`}
       |Name: $code
       |Message: $message
       |Data: ${data.getOrElse("No data")}
       |RequestId: ${requestId.getOrElse("None")}""".stripMargin
}
object UserFriendlyError {
//  final case class InvalidFeatureConfig[T <: FeaturesName](
//    name: T,
//    message: String,
//    cause: Option[Throwable] = None
//  ) extends Exception(s"Invalid feature config for ${name.toString}: $message", cause.orNull)

  /**
   * Internal uncaught errors
   */
  final case class InternalServerError(
    message: String = "An internal error occurred.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.INTERNAL_SERVER_ERROR
  }
  final case class NetworkError(
    message: String = "Network error.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.NetworkError
    val name: ErrorNames                    = ErrorNames.NETWORK_ERROR
  }
  final case class TooManyRequest(
    message: String = "Too many requests.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.TooManyRequests
    val name: ErrorNames                    = ErrorNames.TOO_MANY_REQUEST
  }
  final case class NotFound(
    message: String = "Resource not found.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.NOT_FOUND
  }
  final case class BadRequest(
    message: String = "Bad request.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.BAD_REQUEST
  }
  final case class GraphqlBadRequest(
    command: String,
    info: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.GRAPHQL_BAD_REQUEST
    override def message                    = s"Graphql bad request, code: $command, $info"
  }
  final case class HttpRequestError(info: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.HTTP_REQUEST_ERROR
    override def message                    = s"HTTP request error, message: $info"
  }
  final case class EmailServiceNotConfigured(
    message: String = "Email service is not configured",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.EMAIL_SERVICE_NOT_CONFIGURED
  }

  /**
   * Input errors
   */
  final case class QueryTooLong(max: Long, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.QUERY_TOO_LONG
    override def message                    = s"Query is too long, max length is $max."
  }
  final case class ValidationError(info: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.VALIDATION_ERROR
    override def message                    = s"Validation error, errors: $info."
  }

  /**
   * User errors
   */
  final case class UserNotFound(
    message: String = "User not found.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.USER_NOT_FOUND
  }
  final case class UserAvatarNotFound(
    message: String = "User avatar not found.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.USER_AVATAR_NOT_FOUND
  }
//  final case class EmailAlreadyUsed(
//    message: String = "This email has already been registered.",
//    data: Option[Any] = None,
//    requestId: Option[String] = None
//  ) extends UserFriendlyError {
//    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceAlreadyExists
//    val name: ErrorNames                    = ErrorNames.EMAIL_ALREADY_USED
//  }
  final case class EmailAlreadyUsed(email: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceAlreadyExists
    val name: ErrorNames                    = ErrorNames.EMAIL_ALREADY_USED
    override def message                    = s"This email: $email has already been registered."
  }
  final case class SameEmailProvided(
    message: String = "You are trying to update your account email to the same as the old one.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.SAME_EMAIL_PROVIDED
  }
  final case class WrongSignInCredentials(email: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.WRONG_SIGN_IN_CREDENTIALS
    override def message                    = s"Wrong user email or password: $email."
  }
  final case class UnknownOauthProvider(provider: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.UNKNOWN_OAUTH_PROVIDER
    override def message                    = s"Unknown authentication provider: $provider."
  }
  final case class OauthStateExpired(
    message: String = "OAuth state expired, please try again.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.OAUTH_STATE_EXPIRED
  }
  final case class InvalidOauthCallbackState(
    message: String = "Invalid callback state parameter.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.INVALID_OAUTH_CALLBACK_STATE
  }
  final case class InvalidOauthCallbackCode(
    sta: Int,
    info: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.INVALID_OAUTH_CALLBACK_CODE
    override def message                    =
      s"Invalid callback code parameter, provider response status: $sta and body: $info."
  }
  final case class InvalidAuthState(
    message: String = "Invalid auth state. You might start the auth progress from another device.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.INVALID_AUTH_STATE
  }
  final case class MissingOauthQueryParameter(info: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.MISSING_OAUTH_QUERY_PARAMETER
    override def message                    = s"Missing query parameter: $info."
  }
  final case class OauthAccountAlreadyConnected(
    message: String = "The third-party account has already been connected to another user.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.OAUTH_ACCOUNT_ALREADY_CONNECTED
  }
  final case class InvalidEmail(email: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.INVALID_EMAIL
    override def message                    = s"An invalid email provided: $email."
  }
  final case class InvalidPasswordLength(min: Int, max: Int, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.INVALID_PASSWORD_LENGTH
    override def message                    = s"Password must be between $min and $max characters."
  }
  final case class PasswordRequired(
    message: String = "Password is required.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.PASSWORD_REQUIRED
  }
  case class WrongSignInMethod(
    message: String = "You are trying to sign in by a different method than you signed up with.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.WRONG_SIGN_IN_METHOD
  }
  case class EarlyAccessRequired(
    message: String =
      "You don't have early access permission. Visit https://community.affine.pro/c/insider-general/ for more information.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.EARLY_ACCESS_REQUIRED
  }
  case class SignUpForbidden(
    message: String = "You are not allowed to sign up.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.SIGN_UP_FORBIDDEN
  }
  case class EmailTokenNotFound(
    message: String = "The email token provided is not found.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.EMAIL_TOKEN_NOT_FOUND
  }
  case class InvalidEmailToken(
    message: String = "An invalid email token provided.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.INVALID_EMAIL_TOKEN
  }
  case class LinkExpired(
    message: String = "The link has expired.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.LINK_EXPIRED
  }

  /**
   * Authentication & Permission Errors
   */
  case class AuthenticationRequired(
    message: String = "You must sign in first to access this resource.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.AuthenticationRequired
    val name: ErrorNames                    = ErrorNames.AUTHENTICATION_REQUIRED
  }
  case class ActionForbidden(
    message: String = "You are not allowed to perform this action.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.ACTION_FORBIDDEN
  }
  case class AccessDenied(
    message: String = "You do not have permission to access this resource.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.NoPermission
    val name: ErrorNames                    = ErrorNames.ACCESS_DENIED
  }
  case class EmailVerificationRequired(
    message: String = "You must verify your email before accessing this resource.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.EMAIL_VERIFICATION_REQUIRED
  }

  /**
   * Workspace & Userspace & Doc & Sync errors
   */
  case class WorkspacePermissionNotFound(spaceId: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.WORKSPACE_PERMISSION_NOT_FOUND
    override def message                    = s"Space $spaceId permission not found."
  }
  case class SpaceNotFound(spaceId: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.SPACE_NOT_FOUND
    override def message                    = s"Space $spaceId not found."
  }
  case class MemberNotFoundInSpace(spaceId: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.MEMBER_NOT_FOUND_IN_SPACE
    override def message                    = s"Member not found in Space $spaceId."
  }
  case class NotInSpace(spaceId: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.NOT_IN_SPACE
    override def message                    = s"You should join in Space $spaceId before broadcasting messages."
  }
  case class AlreadyInSpace(spaceId: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.TOO_MANY_REQUEST
    override def message                    = s"You have already joined in Space $spaceId."
  }
  case class SpaceAccessDenied(spaceId: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.NoPermission
    val name: ErrorNames                    = ErrorNames.SPACE_ACCESS_DENIED
    override def message                    = s"You do not have permission to access Space $spaceId."
  }
  case class SpaceOwnerNotFound(spaceId: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.SPACE_OWNER_NOT_FOUND
    override def message                    = s"Owner of Space $spaceId not found."
  }
  case class SpaceShouldHaveOnlyOneOwner(
    message: String = "Space should have only one owner.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.SPACE_SHOULD_HAVE_ONLY_ONE_OWNER
  }
  case class OwnerCanNotLeaveWorkspace(
    message: String = "Owner can not leave the workspace.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.SPACE_SHOULD_HAVE_ONLY_ONE_OWNER
  }
  case class CanNotRevokeYourself(
    message: String = "You can not revoke your own permission.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.SPACE_SHOULD_HAVE_ONLY_ONE_OWNER
  }
  case class DocNotFound(spaceId: String, docId: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.DOC_NOT_FOUND
    override def message                    = s"Doc $docId under Space $spaceId not found."
  }
  case class DocActionDenied(docId: String, action: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.NoPermission
    val name: ErrorNames                    = ErrorNames.DOC_ACTION_DENIED
    override def message                    =
      s"You do not have permission to perform $action action on doc $docId."
  }
  case class DocUpdateBlocked(
    spaceId: String,
    docId: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.DOC_UPDATE_BLOCKED
    override def message                    =
      s"Doc $docId under Space $spaceId is blocked from updating."
  }
  case class VersionRejected(
    version: String,
    serverVersion: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.VERSION_REJECTED
    override def message                    =
      s"Your client with version $version is rejected by remote sync server. Please upgrade to $serverVersion."
  }
  case class InvalidHistoryTimestamp(
    message: String = "Invalid doc history timestamp provided.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.INVALID_HISTORY_TIMESTAMP
  }
  case class DocHistoryNotFound(
    spaceId: String,
    docId: String,
    timestamp: Int,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.DOC_HISTORY_NOT_FOUND
    override def message                    = s"History of $docId at $timestamp under Space $spaceId."
  }
  case class BlobNotFound(spaceId: String, blobId: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.BLOB_NOT_FOUND
    override def message                    = s"Blob $blobId not found in Space $spaceId."
  }
  case class ExpectToPublishDoc(
    message: String = "Expected to publish a doc, not a Space.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.EXPECT_TO_PUBLISH_DOC
  }
  case class ExpectToRevokePublicDoc(
    message: String = "Expected to revoke a public doc, not a Space.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.EXPECT_TO_REVOKE_PUBLIC_DOC
  }
  case class ExpectToGrantDocUserRoles(
    spaceId: String,
    docId: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.EXPECT_TO_GRANT_DOC_USER_ROLES
    override def message                    = s"Expect grant roles on doc $docId under Space $spaceId, not a Space."
  }
  case class ExpectToRevokeDocUserRoles(
    spaceId: String,
    docId: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.EXPECT_TO_REVOKE_DOC_USER_ROLES
    override def message                    = s"Expect revoke roles on doc $docId under Space $spaceId, not a Space."
  }
  case class ExpectToUpdateDocUserRole(
    spaceId: String,
    docId: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.EXPECT_TO_UPDATE_DOC_USER_ROLE
    override def message                    = s"Expect update roles on doc $docId under Space $spaceId, not a Space."
  }
  case class DocIsNotPublic(
    message: String = "Doc is not public.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.DOC_IS_NOT_PUBLIC
  }
  case class FailedToSaveUpdates(
    message: String = "Failed to store doc updates.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.FAILED_TO_SAVE_UPDATES
  }
  case class FailedToUpsertSnapshot(
    message: String = "Failed to store doc snapshot.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.FAILED_TO_UPSERT_SNAPSHOT
  }
  case class ActionForbiddenOnNonTeamWorkspace(
    message: String = "A Team workspace is required to perform this action.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.ACTION_FORBIDDEN_ON_NON_TEAM_WORKSPACE
  }
  case class DocDefaultRoleCanNotBeOwner(
    message: String = "Doc default role can not be owner.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.DOC_DEFAULT_ROLE_CAN_NOT_BE_OWNER
  }
  case class CanNotBatchGrantDocOwnerPermissions(
    message: String = "Can not batch grant doc owner permissions.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.CAN_NOT_BATCH_GRANT_DOC_OWNER_PERMISSIONS
  }
  case class NewOwnerIsNotActiveMember(
    message: String = "Can not set a non-active member as owner.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.NEW_OWNER_IS_NOT_ACTIVE_MEMBER
  }
  case class InvalidInvitation(
    message: String = "Invalid invitation provided.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.NEW_OWNER_IS_NOT_ACTIVE_MEMBER
  }
  case class NoMoreSeat(spaceId: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.NO_MORE_SEAT
    override def message                    = s"No more seat available in the Space $spaceId."
  }

  /**
   * Subscription Errors
   */
  case class UnsupportedSubscriptionPlan(plan: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.UNSUPPORTED_SUBSCRIPTION_PLAN
    override def message                    = s"Unsupported subscription plan: $plan."
  }
  case class FailedToCheckout(
    message: String = "Failed to create checkout session.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.FAILED_TO_CHECKOUT
  }
  case class InvalidCheckoutParameters(
    message: String = "Invalid checkout parameters provided.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.INVALID_CHECKOUT_PARAMETERS
  }
  case class SubscriptionAlreadyExists(plan: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceAlreadyExists
    val name: ErrorNames                    = ErrorNames.SUBSCRIPTION_ALREADY_EXISTS
    override def message                    = s"You have already subscribed to the $plan plan."
  }
  case class InvalidSubscriptionParameters(
    message: String = "Invalid subscription parameters provided.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.INVALID_SUBSCRIPTION_PARAMETERS
  }
  // TODO
  case class SubscriptionNotExists(plan: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.SUBSCRIPTION_NOT_EXISTS
    override def message                    = s"You didn't subscribe to the $plan plan."
  }
  case class SubscriptionHasBeenCanceled(
    message: String = "Your subscription has already been canceled.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.SUBSCRIPTION_HAS_BEEN_CANCELED
  }
  case class SubscriptionHasNotBeenCanceled(
    message: String = "Your subscription has not been canceled.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.SUBSCRIPTION_HAS_NOT_BEEN_CANCELED
  }
  case class SubscriptionExpired(
    message: String = "Your subscription has expired.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.SUBSCRIPTION_EXPIRED
  }
  case class SameSubscriptionRecurring(recurring: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.SAME_SUBSCRIPTION_RECURRING
    override def message                    = s"Your subscription has already been in $recurring recurring state."
  }
  case class CustomerPortalCreateFailed(
    message: String = "Failed to create customer portal session.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.CUSTOMER_PORTAL_CREATE_FAILED
  }
  case class SubscriptionPlanNotFound(
    plan: String,
    recurring: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.SUBSCRIPTION_PLAN_NOT_FOUND
    override def message                    = s"You are trying to access a unknown $recurring subscription $plan plan."
  }
  case class CantUpdateOnetimePaymentSubscription(
    message: String = "You cannot update an onetime payment subscription.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.CANT_UPDATE_ONETIME_PAYMENT_SUBSCRIPTION
  }
  case class WorkspaceIdRequiredForTeamSubscription(
    message: String = "A workspace is required to checkout for team subscription.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.WORKSPACE_ID_REQUIRED_FOR_TEAM_SUBSCRIPTION
  }
  case class WorkspaceIdRequiredToUpdateTeamSubscription(
    message: String = "Workspace id is required to update team subscription.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.WORKSPACE_ID_REQUIRED_TO_UPDATE_TEAM_SUBSCRIPTION
  }

  /**
   *  Copilot errors
   */
  case class CopilotSessionNotFound(
    message: String = "Password is required.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.COPILOT_SESSION_NOT_FOUND
  }
  case class CopilotSessionDeleted(
    message: String = "Copilot session has been deleted.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.COPILOT_SESSION_DELETED
  }
  case class NoCopilotProviderAvailable(
    message: String = "No copilot provider available.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.NO_COPILOT_PROVIDER_AVAILABLE
  }
  case class CopilotFailedToGenerateText(
    message: String = "Failed to generate text.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.COPILOT_FAILED_TO_GENERATE_TEXT
  }
  case class CopilotFailedToCreateMessage(
    message: String = "Failed to create chat message.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.COPILOT_FAILED_TO_CREATE_MESSAGE
  }
  case class UnsplashIsNotConfigured(
    message: String = "Unsplash is not configured.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.UNSPLASH_IS_NOT_CONFIGURED
  }
  case class CopilotActionTaken(
    message: String = "Action has been taken, no more messages allowed.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.COPILOT_ACTION_TAKEN
  }
  case class CopilotDocNotFound(docId: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.COPILOT_DOC_NOT_FOUND
    override def message                    = s"Doc $docId not found."
  }
  case class CopilotDocsNotFound(
    message: String = "Some docs not found.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.COPILOT_DOCS_NOT_FOUND
  }
  case class CopilotMessageNotFound(messageId: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.COPILOT_MESSAGE_NOT_FOUND
    override def message                    = s"Copilot message $messageId not found."
  }
  case class CopilotPromptNotFound(info: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.COPILOT_PROMPT_NOT_FOUND
    override def message                    = s"Copilot prompt $info not found."
  }
  case class CopilotPromptInvalid(
    message: String = "Copilot prompt is invalid.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.COPILOT_PROMPT_INVALID
  }
  case class CopilotProviderSideError(
    provider: String,
    kind: String,
    info: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.COPILOT_PROVIDER_SIDE_ERROR
    override def message                    = s"Provider $provider failed with $kind error: $info"
  }
  case class CopilotInvalidContext(contextId: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.COPILOT_INVALID_CONTEXT
    override def message                    = s"Invalid copilot context $contextId."
  }
  case class CopilotContextFileNotSupported(
    fileName: String,
    info: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.COPILOT_CONTEXT_FILE_NOT_SUPPORTED
    override def message                    = s"File $fileName is not supported to use as context: $info."
  }
  case class CopilotFailedToModifyContext(
    contextId: String,
    info: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.COPILOT_FAILED_TO_MODIFY_CONTEXT
    override def message                    = s"Failed to modify context $contextId: $info"
  }
  case class CopilotFailedToMatchContext(
    contextId: String,
    content: String,
    info: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.COPILOT_FAILED_TO_MATCH_CONTEXT
    override def message                    = s"""Failed to match context $contextId with "${escape(content)}": $info"""
  }
  case class CopilotEmbeddingDisabled(
    message: String =
      "Embedding feature is disabled, please contact the administrator to enable it in the workspace settings.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.COPILOT_EMBEDDING_DISABLED
  }
  case class CopilotEmbeddingUnavailable(
    message: String = "Embedding feature not available, you may need to install pgvector extension to your database.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.COPILOT_EMBEDDING_UNAVAILABLE
  }
  case class CopilotTranscriptionJobExists(
    message: String = "Transcription job already exists.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.COPILOT_TRANSCRIPTION_JOB_EXISTS
  }
  case class CopilotTranscriptionJobNotFound(
    message: String = "Transcription job not found.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.COPILOT_TRANSCRIPTION_JOB_NOT_FOUND
  }
  case class CopilotTranscriptionAudioNotProvided(
    message: String = "Audio not provided.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.COPILOT_TRANSCRIPTION_AUDIO_NOT_PROVIDED
  }
  case class CopilotFailedToAddWorkspaceFileEmbedding(
    info: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.COPILOT_FAILED_TO_ADD_WORKSPACE_FILE_EMBEDDING
    override def message                    = s"Failed to add workspace file embedding: $info"
  }

  /**
   * Quota & Limit errors
   */
  case class BlobQuotaExceeded(
    message: String = "You have exceeded your blob size quota.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.QuotaExceeded
    val name: ErrorNames                    = ErrorNames.BLOB_QUOTA_EXCEEDED
  }
  case class StorageQuotaExceeded(
    message: String = "You have exceeded your storage quota.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.QuotaExceeded
    val name: ErrorNames                    = ErrorNames.STORAGE_QUOTA_EXCEEDED
  }
  case class MemberQuotaExceeded(
    message: String = "You have exceeded your workspace member quota.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.QuotaExceeded
    val name: ErrorNames                    = ErrorNames.MEMBER_QUOTA_EXCEEDED
  }
  case class CopilotQuotaExceeded(
    message: String = "You have reached the limit of actions in this workspace, please upgrade your plan.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.QuotaExceeded
    val name: ErrorNames                    = ErrorNames.COPILOT_QUOTA_EXCEEDED
  }

  /**
   * Config errors
   */
  case class RuntimeConfigNotFound(
    message: String = "Password is required.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.RUNTIME_CONFIG_NOT_FOUND
  }
  case class InvalidRuntimeConfigType(
    key: String,
    want: String,
    get: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.INVALID_RUNTIME_CONFIG_TYPE
    override def message                    = s"Invalid runtime config type  for '$key', want '$want', but get $get."
  }
  case class MailerServiceIsNotConfigured(
    message: String = "Mailer service is not configured.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InternalServerError
    val name: ErrorNames                    = ErrorNames.MAILER_SERVICE_IS_NOT_CONFIGURED
  }
  case class CannotDeleteAllAdminAccount(
    message: String = "Cannot delete all admin accounts.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.CANNOT_DELETE_ALL_ADMIN_ACCOUNT
  }
  case class CannotDeleteOwnAccount(
    message: String = "Cannot delete own account.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.CANNOT_DELETE_OWN_ACCOUNT
  }

  /**
   * captcha errors
   */
  case class CaptchaVerificationFailed(
    message: String = "Captcha verification failed.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.CAPTCHA_VERIFICATION_FAILED
  }

  /**
   * license errors
   */
  case class InvalidLicenseSessionId(
    message: String = "Invalid session id to generate license key.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.INVALID_LICENSE_SESSION_ID
  }
  case class LicenseRevealed(
    message: String = "License key has been revealed. Please check your mail box of the one provided during checkout.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.LICENSE_REVEALED
  }
  case class WorkspaceLicenseAlreadyExists(
    message: String = "Workspace already has a license applied.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.WORKSPACE_LICENSE_ALREADY_EXISTS
  }
  case class LicenseNotFound(
    message: String = "License not found.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.LICENSE_NOT_FOUND
  }
  case class InvalidLicenseToActivate(
    reason: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.INVALID_LICENSE_TO_ACTIVATE
    override def message                    = s"Invalid license to activate. [$reason]"
  }
  case class InvalidLicenseUpdateParams(
    reason: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.INVALID_LICENSE_UPDATE_PARAMS
    override def message                    = s"Invalid license update params. [$reason]"
  }
  case class LicenseExpired(
    message: String = "License has expired.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.BadRequest
    val name: ErrorNames                    = ErrorNames.LICENSE_EXPIRED
  }

  /**
   * version errors
   */
  case class UnsupportedClientVersion(
    clientVersion: String,
    requiredVersion: String,
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.UNSUPPORTED_CLIENT_VERSION
    override def message                    = s"Unsupported client with version [$clientVersion], required version is [$requiredVersion]."
  }

  /**
   * Notification Errors
   */
  case class NotificationNotFound(
    message: String = "Notification not found.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ResourceNotFound
    val name: ErrorNames                    = ErrorNames.NOTIFICATION_NOT_FOUND
  }
  case class mention_user_doc_access_denied(docId: String, data: Option[Any] = None, requestId: Option[String] = None)
      extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.NoPermission
    val name: ErrorNames                    = ErrorNames.MENTION_USER_DOC_ACCESS_DENIED
    override def message                    = s"Mentioned user can not access doc $docId."
  }
  case class MentionUserOneselfDenied(
    message: String = "You can not mention yourself.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.ActionForbidden
    val name: ErrorNames                    = ErrorNames.MENTION_USER_ONESELF_DENIED
  }

  /**
   * app config
   */
  case class InvalidAppConfig(
    message: String = "Invalid app config.",
    data: Option[Any] = None,
    requestId: Option[String] = None
  ) extends UserFriendlyError {
    val baseType: UserFriendlyErrorBaseType = UserFriendlyErrorBaseType.InvalidInput
    val name: ErrorNames                    = ErrorNames.INVALID_APP_CONFIG
  }
}
