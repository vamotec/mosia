package app.mosia.domain.finance

import app.mosia.domain.finance.AnalysisResult.*

import java.time.OffsetDateTime

case class CombinedAnalysisResults (
                                     symbolAnalyses: Map[String, Map[String, AnalysisResult]],
                                     portfolioRisk: Option[RiskAnalysis],
                                     macroContext: MacroAnalysis,
                                     analysisId: String,
                                     generatedAt: OffsetDateTime
                                   )
