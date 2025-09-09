package app.mosia.domain.finance

case class UpdateData (
                        technicalSignal: Option[TechnicalSignal],
                        newsAlert: Option[NewsAlert]
                      )

object UpdateData:
  def withTechnicalSignal(signal: TechnicalSignal): UpdateData =
    UpdateData(Some(signal), None)
  def withNewsAlert(alert: NewsAlert): UpdateData =
    UpdateData(None, Some(alert))