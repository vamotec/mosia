package app.mosia.domain.finance

import app.mosia.domain.finance.AnalysisResult.*

case class ComprehensiveAnalysisResult (
                                         analysisId: String,
                                         symbolAnalyses: Map[String, Map[String, AnalysisResult]], // 股票 -> 分析类型 -> 结果
                                         portfolioAnalysis: Option[RiskAnalysis],
                                         macroAnalysis: MacroAnalysis,
                                         investmentAdvice: InvestmentAdvice,
                                         executionStrategy: ExecutionStrategy,
                                         metadata: AnalysisMetadata
                                       )
