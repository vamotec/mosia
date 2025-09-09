package app.mosia.domain.finance

case class SymbolSentiment(
                            symbol: String,               // 股票代码
                            sentimentScore: Double,       // 情绪评分
                            sources: List[String],        // 数据来源
                            confidence: Double            // 置信度
                          )
