package app.mosia.domain.finance

case class TradeAdviceRequest (
                                userId: String,
                                tradeIntent: TradeIntent,
                                currentPortfolio: Portfolio,  // 需要定义
                                marketConditions: MarketConditions // 需要定义
                              )

case class TradeAdvice(
                        costAnalysis: CostAnalysis,   // 需要定义
                        executionPlan: ExecutionPlan  // 需要定义
                      )

// 未定义的类型（需要进一步实现）
case class Portfolio(
                      // 根据实际需求定义
                    )
case class MarketConditions(
                             // 根据实际需求定义
                           )
case class CostAnalysis(
                         totalCost: Double
                         // 其他成本相关字段
                       )
case class ExecutionPlan(
                          // 执行计划相关字段
                        )