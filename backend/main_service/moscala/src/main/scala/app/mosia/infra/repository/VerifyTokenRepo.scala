package app.mosia.infra.repository

import app.mosia.domain.model.VerifyTokens
import app.mosia.infra.token.TokenType
import zio.Task

import java.util.UUID
import javax.sql.DataSource

trait VerifyTokenRepo:
  def create(
    `type`: TokenType,
    credentials: String,
    ttlInSec: Int = 1800
  ): Task[String]
  def get(
    `type`: TokenType,
    token: String,
    keep: Option[Boolean]
  ): Task[Option[VerifyTokens]]
  def verify(
    `type`: TokenType,
    token: String,
    credential: Option[String] = None,
    keep: Option[String] = None
  ): Task[Option[VerifyTokens]]
  def delete(`type`: TokenType, token: UUID): Task[Long]
  // clean expired tokens
  def cleanExpired(): Task[Long]
