package app.mosia.domain.finance

import scala.concurrent.duration.FiniteDuration

case class ExecutionStrategy (
                               analysisId: String,
                               totalEstimatedCost: Double,
                               recommendedExecutionOrder: List[TradeIntent],
                               riskManagement: RiskManagement,
                               expectedExecutionTime: FiniteDuration
                             )
