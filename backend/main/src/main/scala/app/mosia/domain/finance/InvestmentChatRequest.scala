package app.mosia.domain.finance

case class InvestmentChatRequest (
                                   userId: String,
                                   sessionId: String,
                                   message: String,
                                   contextType: String,
                                   contextData: Map[String, String]
                                 )
