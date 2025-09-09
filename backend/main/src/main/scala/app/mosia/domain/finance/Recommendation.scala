package app.mosia.domain.finance

case class Recommendation (
                            symbol: String,
                            action: String,               // 建议动作（"buy", "sell", "hold"）
                            targetPrice: Option[Double],  // 目标价格
                            confidence: Double            // 建议置信度
                          )
