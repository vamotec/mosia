package app.mosia.domain.finance

import java.time.OffsetDateTime

case class NewsData (
                      headline: String,              // 新闻标题
                      affectedSymbols: List[String], // 受影响的股票代码列表
                      sentimentImpact: Double,       // 情绪影响评分（-1.0 到 1.0，负值为负面影响）
                      source: String,                // 新闻来源
                      summary: String,               // 新闻摘要
                      timestamp: OffsetDateTime             // 新闻发布时间
                    )
