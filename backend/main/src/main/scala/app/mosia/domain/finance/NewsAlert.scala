package app.mosia.domain.finance

case class NewsAlert (
                       headline: String,
                       sentimentImpact: Double,
                       source: String,
                       affectedSymbols: List[String],
                       summary: String
                     )
