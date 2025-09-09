package app.mosia.domain.finance

import scala.concurrent.duration.*

case class AnalysisOptions(
                            outputDetail: String,         // 输出详细程度（"summary", "detailed"）
                            preferredIndicators: List[String] = Nil, // 首选指标
                            timeHorizon: FiniteDuration = 30.days   // 分析时间范围
                          )
