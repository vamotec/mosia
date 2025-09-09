package app.mosia.core.errors

import caliban.CalibanError.ExecutionError
import caliban.Value.StringValue
import caliban.schema.Schema.{ enumSchema, enumValue }
import caliban.introspection.adt.__EnumValue
import caliban.schema.{ ArgBuilder, Schema }

enum ErrorNames:
  case ACCESS_DENIED,
    ACTION_FORBIDDEN,
    ACTION_FORBIDDEN_ON_NON_TEAM_WORKSPACE,
    ALREADY_IN_SPACE,
    AUTHENTICATION_REQUIRED,
    BAD_REQUEST,
    BLOB_NOT_FOUND,
    BLOB_QUOTA_EXCEEDED,
    CANNOT_DELETE_ALL_ADMIN_ACCOUNT,
    CANNOT_DELETE_OWN_ACCOUNT,
    CANT_UPDATE_ONETIME_PAYMENT_SUBSCRIPTION,
    CAN_NOT_BATCH_GRANT_DOC_OWNER_PERMISSIONS,
    CAN_NOT_REVOKE_YOURSELF,
    CAPTCHA_VERIFICATION_FAILED,
    COPILOT_ACTION_TAKEN,
    COPILOT_CONTEXT_FILE_NOT_SUPPORTED,
    COPILOT_DOCS_NOT_FOUND,
    COPILOT_DOC_NOT_FOUND,
    COPILOT_EMBEDDING_DISABLED,
    COPILOT_EMBEDDING_UNAVAILABLE,
    COPILOT_FAILED_TO_ADD_WORKSPACE_FILE_EMBEDDING,
    COPILOT_FAILED_TO_CREATE_MESSAGE,
    COPILOT_FAILED_TO_GENERATE_TEXT,
    COPILOT_FAILED_TO_MATCH_CONTEXT,
    COPILOT_FAILED_TO_MODIFY_CONTEXT,
    COPILOT_INVALID_CONTEXT,
    COPILOT_MESSAGE_NOT_FOUND,
    COPILOT_PROMPT_INVALID,
    COPILOT_PROMPT_NOT_FOUND,
    COPILOT_PROVIDER_SIDE_ERROR,
    COPILOT_QUOTA_EXCEEDED,
    COPILOT_SESSION_DELETED,
    COPILOT_SESSION_NOT_FOUND,
    COPILOT_TRANSCRIPTION_AUDIO_NOT_PROVIDED,
    COPILOT_TRANSCRIPTION_JOB_EXISTS,
    COPILOT_TRANSCRIPTION_JOB_NOT_FOUND,
    CUSTOMER_PORTAL_CREATE_FAILED,
    DOC_ACTION_DENIED,
    DOC_DEFAULT_ROLE_CAN_NOT_BE_OWNER,
    DOC_HISTORY_NOT_FOUND,
    DOC_IS_NOT_PUBLIC,
    DOC_NOT_FOUND,
    DOC_UPDATE_BLOCKED,
    EARLY_ACCESS_REQUIRED,
    EMAIL_ALREADY_USED,
    EMAIL_SERVICE_NOT_CONFIGURED,
    EMAIL_TOKEN_NOT_FOUND,
    EMAIL_VERIFICATION_REQUIRED,
    EXPECT_TO_GRANT_DOC_USER_ROLES,
    EXPECT_TO_PUBLISH_DOC,
    EXPECT_TO_REVOKE_DOC_USER_ROLES,
    EXPECT_TO_REVOKE_PUBLIC_DOC,
    EXPECT_TO_UPDATE_DOC_USER_ROLE,
    FAILED_TO_CHECKOUT,
    FAILED_TO_SAVE_UPDATES,
    FAILED_TO_UPSERT_SNAPSHOT,
    GRAPHQL_BAD_REQUEST,
    HTTP_REQUEST_ERROR,
    INTERNAL_SERVER_ERROR,
    INVALID_APP_CONFIG,
    INVALID_AUTH_STATE,
    INVALID_CHECKOUT_PARAMETERS,
    INVALID_EMAIL,
    INVALID_EMAIL_TOKEN,
    INVALID_HISTORY_TIMESTAMP,
    INVALID_INVITATION,
    INVALID_LICENSE_SESSION_ID,
    INVALID_LICENSE_TO_ACTIVATE,
    INVALID_LICENSE_UPDATE_PARAMS,
    INVALID_OAUTH_CALLBACK_CODE,
    INVALID_OAUTH_CALLBACK_STATE,
    INVALID_PASSWORD_LENGTH,
    INVALID_RUNTIME_CONFIG_TYPE,
    INVALID_SUBSCRIPTION_PARAMETERS,
    LICENSE_EXPIRED,
    LICENSE_NOT_FOUND,
    LICENSE_REVEALED,
    LINK_EXPIRED,
    MAILER_SERVICE_IS_NOT_CONFIGURED,
    MEMBER_NOT_FOUND_IN_SPACE,
    MEMBER_QUOTA_EXCEEDED,
    MENTION_USER_DOC_ACCESS_DENIED,
    MENTION_USER_ONESELF_DENIED,
    MISSING_OAUTH_QUERY_PARAMETER,
    NETWORK_ERROR,
    NEW_OWNER_IS_NOT_ACTIVE_MEMBER,
    NOTIFICATION_NOT_FOUND,
    NOT_FOUND,
    NOT_IN_SPACE,
    NO_COPILOT_PROVIDER_AVAILABLE,
    NO_MORE_SEAT,
    OAUTH_ACCOUNT_ALREADY_CONNECTED,
    OAUTH_STATE_EXPIRED,
    OWNER_CAN_NOT_LEAVE_WORKSPACE,
    PASSWORD_REQUIRED,
    QUERY_TOO_LONG,
    RUNTIME_CONFIG_NOT_FOUND,
    SAME_EMAIL_PROVIDED,
    SAME_SUBSCRIPTION_RECURRING,
    SIGN_UP_FORBIDDEN,
    SPACE_ACCESS_DENIED,
    SPACE_NOT_FOUND,
    SPACE_OWNER_NOT_FOUND,
    SPACE_SHOULD_HAVE_ONLY_ONE_OWNER,
    STORAGE_QUOTA_EXCEEDED,
    SUBSCRIPTION_ALREADY_EXISTS,
    SUBSCRIPTION_EXPIRED,
    SUBSCRIPTION_HAS_BEEN_CANCELED,
    SUBSCRIPTION_HAS_NOT_BEEN_CANCELED,
    SUBSCRIPTION_NOT_EXISTS,
    SUBSCRIPTION_PLAN_NOT_FOUND,
    TOO_MANY_REQUEST,
    UNKNOWN_OAUTH_PROVIDER,
    UNSPLASH_IS_NOT_CONFIGURED,
    UNSUPPORTED_CLIENT_VERSION,
    UNSUPPORTED_SUBSCRIPTION_PLAN,
    USER_AVATAR_NOT_FOUND,
    USER_NOT_FOUND,
    VALIDATION_ERROR,
    VERSION_REJECTED,
    WORKSPACE_ID_REQUIRED_FOR_TEAM_SUBSCRIPTION,
    WORKSPACE_ID_REQUIRED_TO_UPDATE_TEAM_SUBSCRIPTION,
    WORKSPACE_LICENSE_ALREADY_EXISTS,
    WORKSPACE_PERMISSION_NOT_FOUND,
    WRONG_SIGN_IN_CREDENTIALS,
    WRONG_SIGN_IN_METHOD

private val errorList: List[__EnumValue] = List(
  enumValue(name = "ACCESS_DENIED"),
  enumValue(name = "ACTION_FORBIDDEN"),
  enumValue(name = "ACTION_FORBIDDEN_ON_NON_TEAM_WORKSPACE"),
  enumValue(name = "ALREADY_IN_SPACE"),
  enumValue(name = "AUTHENTICATION_REQUIRED"),
  enumValue(name = "BAD_REQUEST"),
  enumValue(name = "BLOB_NOT_FOUND"),
  enumValue(name = "BLOB_QUOTA_EXCEEDED"),
  enumValue(name = "CANNOT_DELETE_ALL_ADMIN_ACCOUNT"),
  enumValue(name = "CANNOT_DELETE_OWN_ACCOUNT"),
  enumValue(name = "CANT_UPDATE_ONETIME_PAYMENT_SUBSCRIPTION"),
  enumValue(name = "CAN_NOT_BATCH_GRANT_DOC_OWNER_PERMISSIONS"),
  enumValue(name = "CAN_NOT_REVOKE_YOURSELF"),
  enumValue(name = "CAPTCHA_VERIFICATION_FAILED"),
  enumValue(name = "COPILOT_ACTION_TAKEN"),
  enumValue(name = "COPILOT_CONTEXT_FILE_NOT_SUPPORTED"),
  enumValue(name = "COPILOT_DOCS_NOT_FOUND"),
  enumValue(name = "COPILOT_DOC_NOT_FOUND"),
  enumValue(name = "COPILOT_EMBEDDING_DISABLED"),
  enumValue(name = "COPILOT_EMBEDDING_UNAVAILABLE"),
  enumValue(name = "COPILOT_FAILED_TO_ADD_WORKSPACE_FILE_EMBEDDING"),
  enumValue(name = "COPILOT_FAILED_TO_CREATE_MESSAGE"),
  enumValue(name = "COPILOT_FAILED_TO_GENERATE_TEXT"),
  enumValue(name = "COPILOT_FAILED_TO_MATCH_CONTEXT"),
  enumValue(name = "COPILOT_FAILED_TO_MODIFY_CONTEXT"),
  enumValue(name = "COPILOT_INVALID_CONTEXT"),
  enumValue(name = "COPILOT_MESSAGE_NOT_FOUND"),
  enumValue(name = "COPILOT_PROMPT_INVALID"),
  enumValue(name = "COPILOT_PROMPT_NOT_FOUND"),
  enumValue(name = "COPILOT_PROVIDER_SIDE_ERROR"),
  enumValue(name = "COPILOT_QUOTA_EXCEEDED"),
  enumValue(name = "COPILOT_SESSION_DELETED"),
  enumValue(name = "COPILOT_SESSION_NOT_FOUND"),
  enumValue(name = "COPILOT_TRANSCRIPTION_AUDIO_NOT_PROVIDED"),
  enumValue(name = "COPILOT_TRANSCRIPTION_JOB_EXISTS"),
  enumValue(name = "COPILOT_TRANSCRIPTION_JOB_NOT_FOUND"),
  enumValue(name = "CUSTOMER_PORTAL_CREATE_FAILED"),
  enumValue(name = "DOC_ACTION_DENIED"),
  enumValue(name = "DOC_DEFAULT_ROLE_CAN_NOT_BE_OWNER"),
  enumValue(name = "DOC_HISTORY_NOT_FOUND"),
  enumValue(name = "DOC_IS_NOT_PUBLIC"),
  enumValue(name = "DOC_NOT_FOUND"),
  enumValue(name = "DOC_UPDATE_BLOCKED"),
  enumValue(name = "EARLY_ACCESS_REQUIRED"),
  enumValue(name = "EMAIL_ALREADY_USED"),
  enumValue(name = "EMAIL_SERVICE_NOT_CONFIGURED"),
  enumValue(name = "EMAIL_TOKEN_NOT_FOUND"),
  enumValue(name = "EMAIL_VERIFICATION_REQUIRED"),
  enumValue(name = "EXPECT_TO_GRANT_DOC_USER_ROLES"),
  enumValue(name = "EXPECT_TO_PUBLISH_DOC"),
  enumValue(name = "EXPECT_TO_REVOKE_DOC_USER_ROLES"),
  enumValue(name = "EXPECT_TO_REVOKE_PUBLIC_DOC"),
  enumValue(name = "EXPECT_TO_UPDATE_DOC_USER_ROLE"),
  enumValue(name = "FAILED_TO_CHECKOUT"),
  enumValue(name = "FAILED_TO_SAVE_UPDATES"),
  enumValue(name = "FAILED_TO_UPSERT_SNAPSHOT"),
  enumValue(name = "GRAPHQL_BAD_REQUEST"),
  enumValue(name = "HTTP_REQUEST_ERROR"),
  enumValue(name = "INTERNAL_SERVER_ERROR"),
  enumValue(name = "INVALID_APP_CONFIG"),
  enumValue(name = "INVALID_AUTH_STATE"),
  enumValue(name = "INVALID_CHECKOUT_PARAMETERS"),
  enumValue(name = "INVALID_EMAIL"),
  enumValue(name = "INVALID_EMAIL_TOKEN"),
  enumValue(name = "INVALID_HISTORY_TIMESTAMP"),
  enumValue(name = "INVALID_INVITATION"),
  enumValue(name = "INVALID_LICENSE_SESSION_ID"),
  enumValue(name = "INVALID_LICENSE_TO_ACTIVATE"),
  enumValue(name = "INVALID_LICENSE_UPDATE_PARAMS"),
  enumValue(name = "INVALID_OAUTH_CALLBACK_CODE"),
  enumValue(name = "INVALID_OAUTH_CALLBACK_STATE"),
  enumValue(name = "INVALID_PASSWORD_LENGTH"),
  enumValue(name = "INVALID_RUNTIME_CONFIG_TYPE"),
  enumValue(name = "INVALID_SUBSCRIPTION_PARAMETERS"),
  enumValue(name = "LICENSE_EXPIRED"),
  enumValue(name = "LICENSE_NOT_FOUND"),
  enumValue(name = "LICENSE_REVEALED"),
  enumValue(name = "LINK_EXPIRED"),
  enumValue(name = "MAILER_SERVICE_IS_NOT_CONFIGURED"),
  enumValue(name = "MEMBER_NOT_FOUND_IN_SPACE"),
  enumValue(name = "MEMBER_QUOTA_EXCEEDED"),
  enumValue(name = "MENTION_USER_DOC_ACCESS_DENIED"),
  enumValue(name = "MENTION_USER_ONESELF_DENIED"),
  enumValue(name = "MISSING_OAUTH_QUERY_PARAMETER"),
  enumValue(name = "NETWORK_ERROR"),
  enumValue(name = "NEW_OWNER_IS_NOT_ACTIVE_MEMBER"),
  enumValue(name = "NOTIFICATION_NOT_FOUND"),
  enumValue(name = "NOT_FOUND"),
  enumValue(name = "NOT_IN_SPACE"),
  enumValue(name = "NO_COPILOT_PROVIDER_AVAILABLE"),
  enumValue(name = "NO_MORE_SEAT"),
  enumValue(name = "OAUTH_ACCOUNT_ALREADY_CONNECTED"),
  enumValue(name = "OAUTH_STATE_EXPIRED"),
  enumValue(name = "OWNER_CAN_NOT_LEAVE_WORKSPACE"),
  enumValue(name = "PASSWORD_REQUIRED"),
  enumValue(name = "QUERY_TOO_LONG"),
  enumValue(name = "RUNTIME_CONFIG_NOT_FOUND"),
  enumValue(name = "SAME_EMAIL_PROVIDED"),
  enumValue(name = "SAME_SUBSCRIPTION_RECURRING"),
  enumValue(name = "SIGN_UP_FORBIDDEN"),
  enumValue(name = "SPACE_ACCESS_DENIED"),
  enumValue(name = "SPACE_NOT_FOUND"),
  enumValue(name = "SPACE_OWNER_NOT_FOUND"),
  enumValue(name = "SPACE_SHOULD_HAVE_ONLY_ONE_OWNER"),
  enumValue(name = "STORAGE_QUOTA_EXCEEDED"),
  enumValue(name = "SUBSCRIPTION_ALREADY_EXISTS"),
  enumValue(name = "SUBSCRIPTION_EXPIRED"),
  enumValue(name = "SUBSCRIPTION_HAS_BEEN_CANCELED"),
  enumValue(name = "SUBSCRIPTION_HAS_NOT_BEEN_CANCELED"),
  enumValue(name = "SUBSCRIPTION_NOT_EXISTS"),
  enumValue(name = "SUBSCRIPTION_PLAN_NOT_FOUND"),
  enumValue(name = "TOO_MANY_REQUEST"),
  enumValue(name = "UNKNOWN_OAUTH_PROVIDER"),
  enumValue(name = "UNSPLASH_IS_NOT_CONFIGURED"),
  enumValue(name = "UNSUPPORTED_CLIENT_VERSION"),
  enumValue(name = "UNSUPPORTED_SUBSCRIPTION_PLAN"),
  enumValue(name = "USER_AVATAR_NOT_FOUND"),
  enumValue(name = "USER_NOT_FOUND"),
  enumValue(name = "VALIDATION_ERROR"),
  enumValue(name = "VERSION_REJECTED"),
  enumValue(name = "WORKSPACE_ID_REQUIRED_FOR_TEAM_SUBSCRIPTION"),
  enumValue(name = "WORKSPACE_ID_REQUIRED_TO_UPDATE_TEAM_SUBSCRIPTION"),
  enumValue(name = "WORKSPACE_LICENSE_ALREADY_EXISTS"),
  enumValue(name = "WORKSPACE_PERMISSION_NOT_FOUND"),
  enumValue(name = "WRONG_SIGN_IN_CREDENTIALS"),
  enumValue(name = "WRONG_SIGN_IN_METHOD")
)

given Schema[Any, ErrorNames] = enumSchema[ErrorNames](
  name = "Color",
  description = Some("Represents a color option"),
  values = errorList,
  directives = List.empty,
  repr = {
    case ErrorNames.ACCESS_DENIED                                     => "ACCESS_DENIED"
    case ErrorNames.ACTION_FORBIDDEN                                  => "ACTION_FORBIDDEN"
    case ErrorNames.ACTION_FORBIDDEN_ON_NON_TEAM_WORKSPACE            => "ACTION_FORBIDDEN_ON_NON_TEAM_WORKSPACE"
    case ErrorNames.ALREADY_IN_SPACE                                  => "ALREADY_IN_SPACE"
    case ErrorNames.AUTHENTICATION_REQUIRED                           => "AUTHENTICATION_REQUIRED"
    case ErrorNames.BAD_REQUEST                                       => "BAD_REQUEST"
    case ErrorNames.BLOB_NOT_FOUND                                    => "BLOB_NOT_FOUND"
    case ErrorNames.BLOB_QUOTA_EXCEEDED                               => "BLOB_QUOTA_EXCEEDED"
    case ErrorNames.CANNOT_DELETE_ALL_ADMIN_ACCOUNT                   => "CANNOT_DELETE_ALL_ADMIN_ACCOUNT"
    case ErrorNames.CANNOT_DELETE_OWN_ACCOUNT                         => "CANNOT_DELETE_OWN_ACCOUNT"
    case ErrorNames.CANT_UPDATE_ONETIME_PAYMENT_SUBSCRIPTION          => "CANT_UPDATE_ONETIME_PAYMENT_SUBSCRIPTION"
    case ErrorNames.CAN_NOT_BATCH_GRANT_DOC_OWNER_PERMISSIONS         => "CAN_NOT_BATCH_GRANT_DOC_OWNER_PERMISSIONS"
    case ErrorNames.CAN_NOT_REVOKE_YOURSELF                           => "CAN_NOT_REVOKE_YOURSELF"
    case ErrorNames.CAPTCHA_VERIFICATION_FAILED                       => "CAPTCHA_VERIFICATION_FAILED"
    case ErrorNames.COPILOT_ACTION_TAKEN                              => "COPILOT_ACTION_TAKEN"
    case ErrorNames.COPILOT_CONTEXT_FILE_NOT_SUPPORTED                => "COPILOT_CONTEXT_FILE_NOT_SUPPORTED"
    case ErrorNames.COPILOT_DOCS_NOT_FOUND                            => "COPILOT_DOCS_NOT_FOUND"
    case ErrorNames.COPILOT_DOC_NOT_FOUND                             => "COPILOT_DOC_NOT_FOUND"
    case ErrorNames.COPILOT_EMBEDDING_DISABLED                        => "COPILOT_EMBEDDING_DISABLED"
    case ErrorNames.COPILOT_EMBEDDING_UNAVAILABLE                     => "COPILOT_EMBEDDING_UNAVAILABLE"
    case ErrorNames.COPILOT_FAILED_TO_ADD_WORKSPACE_FILE_EMBEDDING    => "COPILOT_FAILED_TO_ADD_WORKSPACE_FILE_EMBEDDING"
    case ErrorNames.COPILOT_FAILED_TO_CREATE_MESSAGE                  => "COPILOT_FAILED_TO_CREATE_MESSAGE"
    case ErrorNames.COPILOT_FAILED_TO_GENERATE_TEXT                   => "COPILOT_FAILED_TO_GENERATE_TEXT"
    case ErrorNames.COPILOT_FAILED_TO_MATCH_CONTEXT                   => "COPILOT_FAILED_TO_MATCH_CONTEXT"
    case ErrorNames.COPILOT_FAILED_TO_MODIFY_CONTEXT                  => "COPILOT_FAILED_TO_MODIFY_CONTEXT"
    case ErrorNames.COPILOT_INVALID_CONTEXT                           => "COPILOT_INVALID_CONTEXT"
    case ErrorNames.COPILOT_MESSAGE_NOT_FOUND                         => "COPILOT_MESSAGE_NOT_FOUND"
    case ErrorNames.COPILOT_PROMPT_INVALID                            => "COPILOT_PROMPT_INVALID"
    case ErrorNames.COPILOT_PROMPT_NOT_FOUND                          => "COPILOT_PROMPT_NOT_FOUND"
    case ErrorNames.COPILOT_PROVIDER_SIDE_ERROR                       => "COPILOT_PROVIDER_SIDE_ERROR"
    case ErrorNames.COPILOT_QUOTA_EXCEEDED                            => "COPILOT_QUOTA_EXCEEDED"
    case ErrorNames.COPILOT_SESSION_DELETED                           => "COPILOT_SESSION_DELETED"
    case ErrorNames.COPILOT_SESSION_NOT_FOUND                         => "COPILOT_SESSION_NOT_FOUND"
    case ErrorNames.COPILOT_TRANSCRIPTION_AUDIO_NOT_PROVIDED          => "COPILOT_TRANSCRIPTION_AUDIO_NOT_PROVIDED"
    case ErrorNames.COPILOT_TRANSCRIPTION_JOB_EXISTS                  => "COPILOT_TRANSCRIPTION_JOB_EXISTS"
    case ErrorNames.COPILOT_TRANSCRIPTION_JOB_NOT_FOUND               => "COPILOT_TRANSCRIPTION_JOB_NOT_FOUND"
    case ErrorNames.CUSTOMER_PORTAL_CREATE_FAILED                     => "CUSTOMER_PORTAL_CREATE_FAILED"
    case ErrorNames.DOC_ACTION_DENIED                                 => "DOC_ACTION_DENIED"
    case ErrorNames.DOC_DEFAULT_ROLE_CAN_NOT_BE_OWNER                 => "DOC_DEFAULT_ROLE_CAN_NOT_BE_OWNER"
    case ErrorNames.DOC_HISTORY_NOT_FOUND                             => "DOC_HISTORY_NOT_FOUND"
    case ErrorNames.DOC_IS_NOT_PUBLIC                                 => "DOC_IS_NOT_PUBLIC"
    case ErrorNames.DOC_NOT_FOUND                                     => "DOC_NOT_FOUND"
    case ErrorNames.DOC_UPDATE_BLOCKED                                => "DOC_UPDATE_BLOCKED"
    case ErrorNames.EARLY_ACCESS_REQUIRED                             => "EARLY_ACCESS_REQUIRED"
    case ErrorNames.EMAIL_ALREADY_USED                                => "EMAIL_ALREADY_USED"
    case ErrorNames.EMAIL_SERVICE_NOT_CONFIGURED                      => "EMAIL_SERVICE_NOT_CONFIGURED"
    case ErrorNames.EMAIL_TOKEN_NOT_FOUND                             => "EMAIL_TOKEN_NOT_FOUND"
    case ErrorNames.EMAIL_VERIFICATION_REQUIRED                       => "EMAIL_VERIFICATION_REQUIRED"
    case ErrorNames.EXPECT_TO_GRANT_DOC_USER_ROLES                    => "EXPECT_TO_GRANT_DOC_USER_ROLES"
    case ErrorNames.EXPECT_TO_PUBLISH_DOC                             => "EXPECT_TO_PUBLISH_DOC"
    case ErrorNames.EXPECT_TO_REVOKE_DOC_USER_ROLES                   => "EXPECT_TO_REVOKE_DOC_USER_ROLES"
    case ErrorNames.EXPECT_TO_REVOKE_PUBLIC_DOC                       => "EXPECT_TO_REVOKE_PUBLIC_DOC"
    case ErrorNames.EXPECT_TO_UPDATE_DOC_USER_ROLE                    => "EXPECT_TO_UPDATE_DOC_USER_ROLE"
    case ErrorNames.FAILED_TO_CHECKOUT                                => "FAILED_TO_CHECKOUT"
    case ErrorNames.FAILED_TO_SAVE_UPDATES                            => "FAILED_TO_SAVE_UPDATES"
    case ErrorNames.FAILED_TO_UPSERT_SNAPSHOT                         => "FAILED_TO_UPSERT_SNAPSHOT"
    case ErrorNames.GRAPHQL_BAD_REQUEST                               => "GRAPHQL_BAD_REQUEST"
    case ErrorNames.HTTP_REQUEST_ERROR                                => "HTTP_REQUEST_ERROR"
    case ErrorNames.INTERNAL_SERVER_ERROR                             => "INTERNAL_SERVER_ERROR"
    case ErrorNames.INVALID_APP_CONFIG                                => "INVALID_APP_CONFIG"
    case ErrorNames.INVALID_AUTH_STATE                                => "INVALID_AUTH_STATE"
    case ErrorNames.INVALID_CHECKOUT_PARAMETERS                       => "INVALID_CHECKOUT_PARAMETERS"
    case ErrorNames.INVALID_EMAIL                                     => "INVALID_EMAIL"
    case ErrorNames.INVALID_EMAIL_TOKEN                               => "INVALID_EMAIL_TOKEN"
    case ErrorNames.INVALID_HISTORY_TIMESTAMP                         => "INVALID_HISTORY_TIMESTAMP"
    case ErrorNames.INVALID_INVITATION                                => "INVALID_INVITATION"
    case ErrorNames.INVALID_LICENSE_SESSION_ID                        => "INVALID_LICENSE_SESSION_ID"
    case ErrorNames.INVALID_LICENSE_TO_ACTIVATE                       => "INVALID_LICENSE_TO_ACTIVATE"
    case ErrorNames.INVALID_LICENSE_UPDATE_PARAMS                     => "INVALID_LICENSE_UPDATE_PARAMS"
    case ErrorNames.INVALID_OAUTH_CALLBACK_CODE                       => "INVALID_OAUTH_CALLBACK_CODE"
    case ErrorNames.INVALID_OAUTH_CALLBACK_STATE                      => "INVALID_OAUTH_CALLBACK_STATE"
    case ErrorNames.INVALID_PASSWORD_LENGTH                           => "INVALID_PASSWORD_LENGTH"
    case ErrorNames.INVALID_RUNTIME_CONFIG_TYPE                       => "INVALID_RUNTIME_CONFIG_TYPE"
    case ErrorNames.INVALID_SUBSCRIPTION_PARAMETERS                   => "INVALID_SUBSCRIPTION_PARAMETERS"
    case ErrorNames.LICENSE_EXPIRED                                   => "LICENSE_EXPIRED"
    case ErrorNames.LICENSE_NOT_FOUND                                 => "LICENSE_NOT_FOUND"
    case ErrorNames.LICENSE_REVEALED                                  => "LICENSE_REVEALED"
    case ErrorNames.LINK_EXPIRED                                      => "LINK_EXPIRED"
    case ErrorNames.MAILER_SERVICE_IS_NOT_CONFIGURED                  => "MAILER_SERVICE_IS_NOT_CONFIGURED"
    case ErrorNames.MEMBER_NOT_FOUND_IN_SPACE                         => "MEMBER_NOT_FOUND_IN_SPACE"
    case ErrorNames.MEMBER_QUOTA_EXCEEDED                             => "MEMBER_QUOTA_EXCEEDED"
    case ErrorNames.MENTION_USER_DOC_ACCESS_DENIED                    => "MENTION_USER_DOC_ACCESS_DENIED"
    case ErrorNames.MENTION_USER_ONESELF_DENIED                       => "MENTION_USER_ONESELF_DENIED"
    case ErrorNames.MISSING_OAUTH_QUERY_PARAMETER                     => "MISSING_OAUTH_QUERY_PARAMETER"
    case ErrorNames.NETWORK_ERROR                                     => "NETWORK_ERROR"
    case ErrorNames.NEW_OWNER_IS_NOT_ACTIVE_MEMBER                    => "NEW_OWNER_IS_NOT_ACTIVE_MEMBER"
    case ErrorNames.NOTIFICATION_NOT_FOUND                            => "NOTIFICATION_NOT_FOUND"
    case ErrorNames.NOT_FOUND                                         => "NOT_FOUND"
    case ErrorNames.NOT_IN_SPACE                                      => "NOT_IN_SPACE"
    case ErrorNames.NO_COPILOT_PROVIDER_AVAILABLE                     => "NO_COPILOT_PROVIDER_AVAILABLE"
    case ErrorNames.NO_MORE_SEAT                                      => "NO_MORE_SEAT"
    case ErrorNames.OAUTH_ACCOUNT_ALREADY_CONNECTED                   => "OAUTH_ACCOUNT_ALREADY_CONNECTED"
    case ErrorNames.OAUTH_STATE_EXPIRED                               => "OAUTH_STATE_EXPIRED"
    case ErrorNames.OWNER_CAN_NOT_LEAVE_WORKSPACE                     => "OWNER_CAN_NOT_LEAVE_WORKSPACE"
    case ErrorNames.PASSWORD_REQUIRED                                 => "PASSWORD_REQUIRED"
    case ErrorNames.QUERY_TOO_LONG                                    => "QUERY_TOO_LONG"
    case ErrorNames.RUNTIME_CONFIG_NOT_FOUND                          => "RUNTIME_CONFIG_NOT_FOUND"
    case ErrorNames.SAME_EMAIL_PROVIDED                               => "SAME_EMAIL_PROVIDED"
    case ErrorNames.SAME_SUBSCRIPTION_RECURRING                       => "SAME_SUBSCRIPTION_RECURRING"
    case ErrorNames.SIGN_UP_FORBIDDEN                                 => "SIGN_UP_FORBIDDEN"
    case ErrorNames.SPACE_ACCESS_DENIED                               => "SPACE_ACCESS_DENIED"
    case ErrorNames.SPACE_NOT_FOUND                                   => "SPACE_NOT_FOUND"
    case ErrorNames.SPACE_OWNER_NOT_FOUND                             => "SPACE_OWNER_NOT_FOUND"
    case ErrorNames.SPACE_SHOULD_HAVE_ONLY_ONE_OWNER                  => "SPACE_SHOULD_HAVE_ONLY_ONE_OWNER"
    case ErrorNames.STORAGE_QUOTA_EXCEEDED                            => "STORAGE_QUOTA_EXCEEDED"
    case ErrorNames.SUBSCRIPTION_ALREADY_EXISTS                       => "SUBSCRIPTION_ALREADY_EXISTS"
    case ErrorNames.SUBSCRIPTION_EXPIRED                              => "SUBSCRIPTION_EXPIRED"
    case ErrorNames.SUBSCRIPTION_HAS_BEEN_CANCELED                    => "SUBSCRIPTION_HAS_BEEN_CANCELED"
    case ErrorNames.SUBSCRIPTION_HAS_NOT_BEEN_CANCELED                => "SUBSCRIPTION_HAS_NOT_BEEN_CANCELED"
    case ErrorNames.SUBSCRIPTION_NOT_EXISTS                           => "SUBSCRIPTION_NOT_EXISTS"
    case ErrorNames.SUBSCRIPTION_PLAN_NOT_FOUND                       => "SUBSCRIPTION_PLAN_NOT_FOUND"
    case ErrorNames.TOO_MANY_REQUEST                                  => "TOO_MANY_REQUEST"
    case ErrorNames.UNKNOWN_OAUTH_PROVIDER                            => "UNKNOWN_OAUTH_PROVIDER"
    case ErrorNames.UNSPLASH_IS_NOT_CONFIGURED                        => "UNSPLASH_IS_NOT_CONFIGURED"
    case ErrorNames.UNSUPPORTED_CLIENT_VERSION                        => "UNSUPPORTED_CLIENT_VERSION"
    case ErrorNames.UNSUPPORTED_SUBSCRIPTION_PLAN                     => "UNSUPPORTED_SUBSCRIPTION_PLAN"
    case ErrorNames.USER_AVATAR_NOT_FOUND                             => "USER_AVATAR_NOT_FOUND"
    case ErrorNames.USER_NOT_FOUND                                    => "USER_NOT_FOUND"
    case ErrorNames.VALIDATION_ERROR                                  => "VALIDATION_ERROR"
    case ErrorNames.VERSION_REJECTED                                  => "VERSION_REJECTED"
    case ErrorNames.WORKSPACE_ID_REQUIRED_FOR_TEAM_SUBSCRIPTION       => "WORKSPACE_ID_REQUIRED_FOR_TEAM_SUBSCRIPTION"
    case ErrorNames.WORKSPACE_ID_REQUIRED_TO_UPDATE_TEAM_SUBSCRIPTION =>
      "WORKSPACE_ID_REQUIRED_TO_UPDATE_TEAM_SUBSCRIPTION"
    case ErrorNames.WORKSPACE_LICENSE_ALREADY_EXISTS                  => "WORKSPACE_LICENSE_ALREADY_EXISTS"
    case ErrorNames.WORKSPACE_PERMISSION_NOT_FOUND                    => "WORKSPACE_PERMISSION_NOT_FOUND"
    case ErrorNames.WRONG_SIGN_IN_CREDENTIALS                         => "WRONG_SIGN_IN_CREDENTIALS"
    case ErrorNames.WRONG_SIGN_IN_METHOD                              => "WRONG_SIGN_IN_METHOD"
  }
)

given ArgBuilder[ErrorNames] =
  case StringValue(str) =>
    ErrorNames.values.find(_.toString == str) match {
      case Some(value) => Right(value)
      case None        => Left(ExecutionError(s"Invalid ErrorNames value: $str"))
    }
  case other            => Left(ExecutionError(s"Expected a string for ErrorNames, but got: $other"))
