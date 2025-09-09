package app.mosia.domain.finance

case class AnalysisMetadata (
                              processingTimeMs: Int,
                              dataQualityScore: Double,
                              analystParticipation: Map[String, Boolean]
                            )
