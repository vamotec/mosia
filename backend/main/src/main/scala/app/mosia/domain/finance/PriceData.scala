package app.mosia.domain.finance

import java.time.OffsetDateTime

// 价格数据，包含实时价格更新信息
case class PriceData(
                      symbol: String, // 股票代码
                      currentPrice: Double, // 当前价格
                      priceChangePercent: Double, // 价格变动百分比
                      volume: Long, // 成交量
                      volumeRatio: Double, // 成交量比率（相对于平均成交量）
                      breakoutSignal: Option[String], // 技术突破信号（例如 "support_break" 或 "resistance_break"）
                      timestamp: OffsetDateTime // 数据时间戳
                    )
