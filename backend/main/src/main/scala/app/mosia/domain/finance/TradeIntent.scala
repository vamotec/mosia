package app.mosia.domain.finance

case class TradeIntent (
                         symbol: String,
                         action: String,
                         targetValue: Double,
                         orderType: String,
                         urgency: String
                       )
