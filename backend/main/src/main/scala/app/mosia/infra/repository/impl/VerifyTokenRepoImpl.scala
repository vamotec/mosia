package app.mosia.infra.repository.impl

import app.mosia.core.configs.AppConfig
import app.mosia.domain.model.VerifyTokens
import app.mosia.domain.model.input.VerifyTokenInput
import app.mosia.infra.dao.DaoModule
import app.mosia.infra.dao.impl.DaoModuleImpl
import app.mosia.infra.eventbus.EventBus
import app.mosia.infra.helpers.crypto.{ CryptoHelper, CryptoHelperImpl }
import app.mosia.infra.repository.VerifyTokenRepo
import app.mosia.infra.token.TokenType
import app.mosia.mapper.DomainMappers.toDomain
import zio.*

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

case class VerifyTokenRepoImpl(dao: DaoModule, crypto: CryptoHelper, ds: DataSource) extends VerifyTokenRepo:

  override def create(
    `type`: TokenType,
    credentials: String,
    ttlInSec: Int = 30 * 60
  ): Task[String] =
    for
      plaintextToken <- ZIO.succeed(UUID.randomUUID())
      now             = Instant.now()
      expiresAt       = now.plusSeconds(ttlInSec)
      // 插入数据库
      _              <- dao.verifyTokenDao
                          .create(
                            VerifyTokenInput(
                              tokenType = `type`,
                              token = plaintextToken,
                              credential = Some(credentials),
                              expiresAt = expiresAt
                            )
                          )
                          .provideEnvironment(ZEnvironment(ds))
      // 加密返回
      encrypted      <- crypto.encrypt(plaintextToken.toString)
    yield encrypted

  /**
   * get token by type
   *
   * token will be deleted if expired or keep is not set
   */
  override def get(
    `type`: TokenType,
    token: String,
    keep: Option[Boolean]
  ): Task[Option[VerifyTokens]] =
    for
      decrypted <- crypto.decrypt(token)
      uuid       = UUID.fromString(decrypted)
      recordOpt <- dao.verifyTokenDao.findUnique(uuid, `type`.toInt).provideEnvironment(ZEnvironment(ds))
      db        <- recordOpt match {
                     case None         => ZIO.none
                     case Some(record) =>
                       val isExpired    = record.expiresAt.isBefore(Instant.now())
                       val shouldDelete = isExpired || !keep.contains(true)
                       for {
                         _ <- ZIO.when(shouldDelete) {
                                delete(`type`, uuid).flatMap {
                                  case 0 => ZIO.none // 被使用或已删除
                                  case _ => ZIO.unit
                                }
                              }
                       } yield if (isExpired) None else Some(record)
                   }
      result    <- ZIO.foreach(db)(toDomain)
    yield result

  /**
   * get token and verify credential
   *
   * if credential is not provided, it will be failed
   *
   * token will be deleted if expired or keep is not set
   */
  override def verify(
    `type`: TokenType,
    token: String,
    credential: Option[String] = None,
    keep: Option[String] = None
  ): Task[Option[VerifyTokens]] =
    for
      record <- get(`type`, token, Some(true))
      result <- record match
                  case Some(v) =>
                    val valid = v.credential.isEmpty || v.credential == credential
                    if (valid && !keep.contains(true))
                      for {
                        count <- delete(`type`, v.token)
                        res   <- if (count > 0) ZIO.succeed(record) else ZIO.none // 如果已删除返回 Some(record)，否则 None（被用过）
                      } yield res
                    else ZIO.succeed(if (valid) record else None)
                  case None    => ZIO.none
    yield result

  override def delete(`type`: TokenType, token: UUID): Task[Long] =
    for {
      count <- dao.verifyTokenDao
                 .deleteMany(token, `type`.toInt)
                 .provideEnvironment(ZEnvironment(ds))
      _     <- ZIO
                 .logInfo(s"Deleted token success by type ${`type`} and token $token")
                 .when(count > 0)
    } yield count

  override def cleanExpired(): Task[Long] =
    for {
      count <- dao.verifyTokenDao
                 .deleteExpired()
                 .provideEnvironment(ZEnvironment(ds))
      _     <- ZIO
                 .logInfo(s"Cleaned $count expired tokens")
                 .when(count > 0)
    } yield count

object VerifyTokenRepoImpl:
  def make: ZIO[CryptoHelper & DaoModule & DataSource, Nothing, VerifyTokenRepo] =
    for
      dao    <- ZIO.service[DaoModule]
      crypto <- ZIO.service[CryptoHelper]
      ds     <- ZIO.service[DataSource]
    yield new VerifyTokenRepoImpl(dao, crypto, ds)

  val live: ZLayer[DataSource & EventBus & Ref[AppConfig], Throwable, VerifyTokenRepo] =
    DaoModuleImpl.layer ++ CryptoHelperImpl.live >>> ZLayer.fromZIO(make)
