package app.mosia.infra.workspace

import java.time.Instant

final case class PaginationInput(
  after: Option[Instant],
  first: Option[Int],
  offset: Option[Int]
)
