package app.mosia.infra.cache

import app.mosia.core.redis.RedisPrefix
import zio.*
import zio.json.*
import zio.redis.*

final case class CacheNamespaceImpl(prefix: RedisPrefix, redis: Redis) extends CacheNamespace {
  private def prefixed(key: String): String                                    = prefix(key)
  // Standard operations
  override def get[T: JsonCodec](key: String): ZIO[Any, RedisError, Option[T]] =
    redis
      .get(prefixed(key))
      .returning[String]
      .map {
        case Some(json) => json.fromJson[T].toOption
        case None       => None
      }
      .catchAll(_ => ZIO.succeed(None))

  override def set[T: JsonCodec](key: String, value: T, opts: CacheSetOptions): ZIO[Any, RedisError, Boolean] = {
    val json = value.toJson
    redis
      .set[String, String](prefixed(key), json, opts.ttl)
      .catchAll(_ => ZIO.succeed(false))
  }

  override def increase(key: String, count: Long): ZIO[Any, RedisError, Long] =
    redis.incrBy(prefixed(key), count).catchAll(_ => ZIO.succeed(0L))

  override def decrease(key: String, count: Long): ZIO[Any, RedisError, Long] =
    redis.decrBy(prefixed(key), count).catchAll(_ => ZIO.succeed(0L))

  override def setnx[T: JsonCodec](key: String, value: T): ZIO[Any, RedisError, Boolean] = {
    val json = value.toJson
    redis
      .setNx[String, String](prefixed(key), json)
      .catchAll(_ => ZIO.succeed(false))
  }

  override def delete(key: String): ZIO[Any, RedisError, Boolean] =
    redis.del(prefixed(key)).map(_ > 0).catchAll(_ => ZIO.succeed(false))

  override def has(key: String): ZIO[Any, RedisError, Boolean] =
    redis.exists(prefixed(key)).map(_ > 0).catchAll(_ => ZIO.succeed(false))

  override def ttl(key: String): ZIO[Any, RedisError, Duration] = redis.ttl(prefixed(key))

  override def expire(key: String, ttl: Duration): ZIO[Any, RedisError, Boolean] =
    redis
      .pExpire(prefixed(key), ttl)
      .catchAll(_ => ZIO.succeed(false))

  // List operations
  override def pushBack[T: JsonCodec](key: String, values: T*): ZIO[Any, RedisError, Long] = {
    val encode: T => ZIO[Any, RedisError, String] =
      value =>
        ZIO.attempt(value.toJson).mapError { err =>
          RedisError.ProtocolError(s"JSON encoding failed: ${err.getMessage}")
        }

    for {
      encoded <- ZIO.foreach(values)(encode)
      result  <- encoded match {
                   case head +: tail => redis.rPush[String, String](prefixed(key), head, tail: _*)
                   case Nil          => ZIO.succeed(0L)
                 }
    } yield result
  }

  override def pushFront[T: JsonCodec](key: String, values: T*): ZIO[Any, RedisError, Long] = {
    val encode: T => ZIO[Any, RedisError, String] =
      value =>
        ZIO.attempt(value.toJson).mapError { err =>
          RedisError.ProtocolError(s"JSON encoding failed: ${err.getMessage}")
        }

    for {
      encoded <- ZIO.foreach(values)(encode)
      result  <- encoded match {
                   case head +: tail => redis.lPush[String, String](prefixed(key), head, tail: _*)
                   case Nil          => ZIO.succeed(0L)
                 }
    } yield result
  }

  override def len(key: String): ZIO[Any, RedisError, Long] =
    redis.lLen(prefixed(key)).catchAll(_ => ZIO.succeed(0L))

  override def list[T: JsonCodec](key: String, start: Long, end: Long): ZIO[Any, RedisError, List[T]] = {
    val range = Range(start.toInt, end.toInt)
    redis
      .lRange(prefixed(key), range)
      .returning[String]
      .map(_.flatMap(_.fromJson[T].toOption).toList)
      .catchAll(_ => ZIO.succeed(List.empty))
  }

  override def popFront[T: JsonCodec](key: String, count: Int): ZIO[Any, RedisError, List[T]] = {
    val range = Range(0, count - 1)
    for {
      raw   <- redis.lRange(prefixed(key), range).returning[String]
      parsed = raw.flatMap(_.fromJson[T].toOption).toList
      _     <- redis.lTrim(prefixed(key), Range(count, -1)) // 保留剩下的
    } yield parsed
  }

  override def popBack[T: JsonCodec](key: String, count: Int): ZIO[Any, RedisError, List[T]] =
    for {
      // 获取列表总长度
      len   <- redis.lLen(prefixed(key))
      // 计算 start 和 end（从右往左取 count 个元素）
      start  = (len - count) max 0
      end    = len - 1
      // 从右侧拿出元素
      raw   <- redis.lRange(prefixed(key), Range(start.toInt, end.toInt)).returning[String]
      parsed = raw.flatMap(_.fromJson[T].toOption).toList
      // 删除这些元素（保留左侧部分）
      _     <- redis.lTrim(prefixed(key), Range(0, (start.toInt - 1) max -1))
    } yield parsed

  // Map (hash) operations
  override def mapSet[T: JsonCodec](map: String, key: String, value: T): ZIO[Any, RedisError, Boolean] = {
    val json = value.toJson
    redis
      .hSet(map, (prefixed(key), json))
      .map(_ > 0)
      .catchAll(_ => ZIO.succeed(false))
  }

  override def mapIncrease(map: String, key: String, count: Long): ZIO[Any, RedisError, Long] =
    redis.hIncrBy(map, prefixed(key), count).catchAll(_ => ZIO.succeed(0L))

  override def mapDecrease(map: String, key: String, count: Long): ZIO[Any, RedisError, Long] =
    redis.hIncrBy(map, prefixed(key), -count).catchAll(_ => ZIO.succeed(0L))

  override def mapGet[T: JsonCodec](map: String, key: String): ZIO[Any, RedisError, Option[T]] =
    redis
      .hGet(map, prefixed(key))
      .returning[String]
      .map(_.flatMap(json => json.fromJson[T].toOption))
      .catchAll(_ => ZIO.succeed(None))

  override def mapDelete(map: String, key: String): ZIO[Any, RedisError, Boolean] =
    redis.hDel(map, prefixed(key)).map(_ > 0).catchAll(_ => ZIO.succeed(false))

  override def mapKeys(map: String): ZIO[Any, RedisError, List[String]] =
    redis
      .hKeys(map)
      .returning[String]
      .map(_.toList)
      .catchAll(_ => ZIO.succeed(List.empty))

  override def mapRandomKey(map: String): ZIO[Any, RedisError, Option[String]] =
    redis
      .hRandField(map)
      .returning[String]
      .catchAll(_ => ZIO.succeed(None))

  override def mapLen(map: String): ZIO[Any, RedisError, Long] =
    redis.hLen(map).catchAll(_ => ZIO.succeed(0L))
}
