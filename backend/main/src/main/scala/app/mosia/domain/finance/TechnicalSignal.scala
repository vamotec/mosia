package app.mosia.domain.finance

case class TechnicalSignal(
                            indicator: String,             // 技术指标名称（如 "rsi", "macd"）
                            value: Double,                // 指标值
                            signalType: String,           // 信号类型（"buy", "sell", "hold"）
                            strength: Double              // 信号强度
                          )
