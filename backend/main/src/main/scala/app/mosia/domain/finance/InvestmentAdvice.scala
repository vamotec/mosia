package app.mosia.domain.finance

import java.time.OffsetDateTime

case class InvestmentAdvice (
                              userId: String,
                              analysisId: String,
                              advice: String,               // 建议文本
                              recommendations: List[Recommendation],
                              riskWarnings: List[String],
                              executionSteps: List[String],
                              confidence: Double,
                              validUntil: OffsetDateTime
                            )
