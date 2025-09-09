package app.mosia.domain.finance

import java.time.OffsetDateTime

sealed trait AnalysisResult:
  def analysisId: String         // 分析ID
  def symbol: String            // 分析的股票代码
  def timestamp: OffsetDateTime         // 分析生成时间

object AnalysisResult:
  case class FundamentalAnalysis(
                                  analysisId: String,
                                  symbol: String,
                                  timestamp: OffsetDateTime,
                                  valuation: Map[String, Double], // 估值指标（如 P/E 比率、DCF估值等）
                                  financialHealth: Double,        // 财务健康评分
                                  growthMetrics: Map[String, Double], // 增长指标（如营收增长率、利润增长率）
                                  breakdown: String               // 详细分析描述
                                ) extends AnalysisResult

  case class TechnicalAnalysis(
                                analysisId: String,
                                symbol: String,
                                timestamp: OffsetDateTime,
                                signals: List[TechnicalSignal], // 技术指标信号
                                timeframe: String, // 分析时间框架（如 "1m", "1h", "1d"）
                                trend: String, // 趋势方向（"bullish", "bearish", "neutral"）
                                breakdown: String // 详细技术分析描述
                              ) extends AnalysisResult

  case class SentimentAnalysis(
                                analysisId: String,
                                symbol: String,
                                timestamp: OffsetDateTime,
                                symbolSentiments: List[SymbolSentiment], // 各股票的情绪分析
                                overallSentiment: Double, // 总体情绪评分
                                breakdown: String // 详细情绪分析描述
                              ) extends AnalysisResult

  case class RiskAnalysis(
                           analysisId: String,
                           symbol: String,
                           timestamp: OffsetDateTime,
                           portfolioId: String, // 投资组合ID
                           riskScore: Double, // 风险评分
                           riskFactors: Map[String, Double], // 风险因子（如波动率、市场风险等）
                           breakdown: String // 详细风险分析描述
                         ) extends AnalysisResult

  case class MacroAnalysis(
                            analysisId: String,
                            symbol: String,
                            timestamp: OffsetDateTime,
                            economicIndicators: Map[String, Double], // 宏观经济指标（如GDP增长率、利率）
                            marketConditions: String, // 市场状况描述
                            breakdown: String // 详细宏观分析描述
                          ) extends AnalysisResult

  case class AnalysisFailure(
                              analysisId: String,
                              symbol: String,
                              timestamp: OffsetDateTime,
                              errorMessage: String // 分析失败的错误信息
                            ) extends AnalysisResult