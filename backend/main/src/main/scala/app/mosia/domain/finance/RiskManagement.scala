package app.mosia.domain.finance

case class RiskManagement (
                            stopLoss: Option[Double],     // 止损点
                            takeProfit: Option[Double],   // 止盈点
                            positionSizing: Double        // 仓位规模
                          )
