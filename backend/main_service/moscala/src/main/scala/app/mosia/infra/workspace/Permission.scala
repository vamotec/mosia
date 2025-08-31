package app.mosia.infra.workspace

import zio.json.JsonCodec

enum Permission derives JsonCodec:
  case Admin, Collaborator, External, Owner

  def toInt: Int = this match
    case Admin        => 10
    case Collaborator => 1
    case External     => -99
    case Owner        => 99

object Permission:
  def fromInt(i: Int): Option[Permission] = i match
    case 10  => Some(Admin)
    case 1   => Some(Collaborator)
    case -99 => Some(External)
    case 99  => Some(Owner)
    case _   => None
