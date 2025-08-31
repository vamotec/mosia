package app.mosia.infra.helpers.server

import app.mosia.core.errors.*
import app.mosia.core.types.JSONValue
import zio.json.ast.Json

trait ConfigDescriptorEntry:
  def validate(value: JSONValue): Either[ErrorMessage, Unit]

object ConfigDescriptorEntry:
  private def validateString(msg: String): ConfigDescriptorEntry =
    case JSONValue.JSONString(s) if s.nonEmpty => Right(())
    case _                                     => Left(ErrorMessage(List(msg)))

  private def validateBoolean(msg: String): ConfigDescriptorEntry =
    case JSONValue.JSONBoolean(_) => Right(())
    case _                        => Left(ErrorMessage(List(msg)))

  private def validatePositiveInt(msg: String): ConfigDescriptorEntry =
    case JSONValue.JSONNumber(n) if n.isValidInt && n.toInt > 0 => Right(())
    case _                                                      => Left(ErrorMessage(List(msg)))

  private def validatePositiveLong(msg: String): ConfigDescriptorEntry =
    case JSONValue.JSONNumber(n) if n.isValidLong && n.toLong > 0 => Right(())
    case _                                                        => Left(ErrorMessage(List(msg)))

  private def validatePort(msg: String): ConfigDescriptorEntry =
    case JSONValue.JSONNumber(n) if n.isValidInt =>
      val i = n.toInt
      if (i >= 1 && i <= 65535) Right(())
      else Left(ErrorMessage(List(msg)))
    case _                                       => Left(ErrorMessage(List(msg)))

  val APP_CONFIG_DESCRIPTORS: Map[String, Map[String, ConfigDescriptorEntry]] = Map(
    "redis"           -> Map(
      "host" -> validateString("redis.host must be a non-empty string"),
      "port" -> validatePort("redis.port must be between 1 and 65535")
    ),
    "database"        -> Map(
      // "dataSourceClassName" -> validateString("database.dataSourceClassName must be a non-empty string"),
      "jdbcUrl"  -> validateString("database.jdbcUrl must be a non-empty string"),
      "username" -> validateString("database.username must be a non-empty string"),
      "password" -> validateString("database.password must be a non-empty string")
    ),
    "storage"         -> Map(
      "default"            -> validateString("storage.default must be a non-empty string"),
      "fs.path"            -> validateString("storage.fs.path must be a non-empty string"),
      "fs.createIfMissing" -> validateBoolean("storage.fs.createIfMissing must be a boolean"),
      "s3.region"          -> validateString("storage.s3.region must be a non-empty string"),
      "s3.accessKeyId"     -> validateString("storage.s3.accessKeyId must be a string"),
      "s3.secretAccessKey" -> validateString("storage.s3.secretAccessKey must be a string"),
      "r2.region"          -> validateString("storage.r2.region must be a string"),
      "r2.accountId"       -> validateString("storage.r2.accountId must be a string"),
      "r2.accessKeyId"     -> validateString("storage.r2.accessKeyId must be a string"),
      "r2.secretAccessKey" -> validateString("storage.r2.secretAccessKey must be a string")
    ),
    "storages.avatar" -> Map(
      "storage.provider" -> validateString("storages.avatar.storage.provider must be a string"),
      "storage.bucket"   -> validateString("storages.avatar.storage.bucket must be a string"),
      "publicPath"       -> validateString("storages.avatar.publicPath must be a string")
    ),
    "storages.blob"   -> Map(
      "storage.provider" -> validateString("storages.blob.storage.provider must be a string"),
      "storage.bucket"   -> validateString("storages.blob.storage.bucket must be a string")
    ),
    "auth"            -> Map(
      "session.ttl"                    -> validatePositiveLong("auth.session.ttl must be a positive number"),
      "session.ttr"                    -> validatePositiveLong("auth.session.ttr must be a positive number"),
      "allowSignup"                    -> validateBoolean("auth.allowSignup must be a boolean"),
      "requireEmailDomainVerification" -> validateBoolean("auth.requireEmailDomainVerification must be a boolean"),
      "requireEmailVerification"       -> validateBoolean("auth.requireEmailVerification must be a boolean"),
      "password.min"                   -> validatePositiveInt("auth.password.min must be a positive integer"),
      "password.max"                   -> validatePositiveInt("auth.password.max must be a positive integer")
    ),
    "flags"           -> Map(
      "earlyAccessControl" -> validateBoolean("flags.earlyAccessControl must be a boolean")
    ),
    "doc"             -> Map(
      "history.interval"   -> validatePositiveLong("doc.history.interval must be a positive number"),
      "experimental.yocto" -> validateBoolean("doc.experimental.yocto must be a boolean")
    ),
    "mailer"          -> Map(
      "smtpHost" -> validateString("mailer.smtpHost must be a non-empty string"),
      "smtpPort" -> validatePort("mailer.smtpPort must be between 1 and 65535"),
      "username" -> validateString("mailer.username must be a non-empty string"),
      "password" -> validateString("mailer.password must be a non-empty string")
    ),
    "server"          -> Map(
      "externalUrl" -> validateString("server.externalUrl must be a string"),
      "https"       -> validateBoolean("server.https must be a boolean"),
      "host"        -> validateString("server.host must be a string"),
      "port"        -> validatePort("server.port must be between 1 and 65535"),
      "path"        -> validateString("server.path must be a string"),
      "name"        -> validateString("server.name must be a string")
    )
  )
