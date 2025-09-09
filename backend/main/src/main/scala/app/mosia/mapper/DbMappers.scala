package app.mosia.mapper

import io.scalaland.chimney.dsl.*
import zio.{ Task, ZIO }

import java.time.ZoneOffset

object DbMappers:
  trait ToDb[T, E]:
    extension (t: T) def toDb: Task[E]

  object ToDb:
    def apply[T, E](using ev: ToDb[T, E]): ToDb[T, E] = ev

  extension [T, E](doList: List[T])
    def toDbList(using ToDb[T, E]): Task[List[E]] =
      ZIO.foreach(doList)(_.toDb)

  extension [T, E](value: T)(using toDtoEv: ToDb[T, E])
    def toDb: Task[E] =
      toDtoEv.toDb(value)

//  given ToDb[Stock, DbStock] with
//    extension (domain: Stock)
//      def toDb: Task[DbStock] =
//        ZIO.attempt:
//          domain
//            .into[DbStock]
//            .withFieldComputed(_.market, _.market.toString)
//            .transform
//
//  given ToDb[KLineData, DbKLineData] with
//    extension (domain: KLineData)
//      def toDb: Task[DbKLineData] =
//        ZIO.attempt:
//          domain
//            .into[DbKLineData]
//            .withFieldComputed(_.market, _.market.toString)
//            .withFieldComputed(_.date, _.date.toInstant(ZoneOffset.UTC))
//            .transform
