package app.mosia.domain.finance

import java.time.OffsetDateTime

case class RecommendationUpdate (
                                  updateId: String,
                                  updateType: String,
                                  affectedSymbols: Seq[String],
                                  message: String,
                                  priority: Priority,
                                  data: UpdateData,
                                  timestamp: OffsetDateTime
                                )
