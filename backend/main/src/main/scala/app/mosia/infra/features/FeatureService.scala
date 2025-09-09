package app.mosia.infra.features

import app.mosia.domain.model.Email.Email
import app.mosia.domain.model.UserFeatures
import app.mosia.infra.auth.Types.EarlyAccessType
import zio.*

import java.util.UUID

trait FeatureService:
  // ======== Admin ========
  def isStaff(email: Email): Task[Boolean]

  def isAdmin(userId: UUID): Task[Boolean]

  def addAdmin(userId: UUID): Task[UserFeatures]

  // ======== Early Access ========
  def addEarlyAccess(userId: UUID, `type`: EarlyAccessType): Task[UserFeatures]

  def removeEarlyAccess(userId: UUID, `type`: EarlyAccessType = EarlyAccessType.App): Task[Unit]

  def isEarlyAccessUser(userId: UUID, `type`: EarlyAccessType = EarlyAccessType.App): Task[Boolean]

  def canEarlyAccess(email: Email, `type`: EarlyAccessType = EarlyAccessType.App): Task[Boolean]
