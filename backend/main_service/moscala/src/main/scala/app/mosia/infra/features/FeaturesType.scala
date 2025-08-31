package app.mosia.infra.features

enum FeaturesType:
  case Feature, Quota

  def toInt: Int = this match
    case Feature => 0
    case Quota   => 1

object FeaturesType:
  def fromInt(i: Int): Option[FeaturesType] = i match
    case 0 => Some(Feature)
    case 1 => Some(Quota)
    case _ => None
