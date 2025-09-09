package app.mosia.infra.helpers.extractor

import app.mosia.domain.model.CurrentUser
import zio.UIO
import zio.http.Request

trait CurrentUserExtractor:
  def getCurrent(request: Request): UIO[CurrentUser]
