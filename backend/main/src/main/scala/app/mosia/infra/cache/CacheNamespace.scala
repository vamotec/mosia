package app.mosia.infra.cache

import zio.*
import zio.json.*
import zio.redis.*

trait CacheNamespace {
  // Standard operations
  def get[T: JsonCodec](key: String): ZIO[Any, RedisError, Option[T]]
  def set[T: JsonCodec](key: String, value: T, opts: CacheSetOptions): ZIO[Any, RedisError, Boolean]
  def increase(key: String, count: Long): ZIO[Any, RedisError, Long]
  def decrease(key: String, count: Long): ZIO[Any, RedisError, Long]
  def setnx[T: JsonCodec](key: String, value: T): ZIO[Any, RedisError, Boolean]
  def delete(key: String): ZIO[Any, RedisError, Boolean]
  def has(key: String): ZIO[Any, RedisError, Boolean]
  def ttl(key: String): ZIO[Any, RedisError, Duration]
  def expire(key: String, ttl: Duration): ZIO[Any, RedisError, Boolean]
  // List operations
  def pushBack[T: JsonCodec](key: String, values: T*): ZIO[Any, RedisError, Long]
  def pushFront[T: JsonCodec](key: String, values: T*): ZIO[Any, RedisError, Long]
  def len(key: String): ZIO[Any, RedisError, Long]
  def list[T: JsonCodec](key: String, start: Long, end: Long): ZIO[Any, RedisError, List[T]]
  def popFront[T: JsonCodec](key: String, count: Int): ZIO[Any, RedisError, List[T]]
  def popBack[T: JsonCodec](key: String, count: Int): ZIO[Any, RedisError, List[T]]
  // Map (hash) operations
  def mapSet[T: JsonCodec](map: String, key: String, value: T): ZIO[Any, RedisError, Boolean]
  def mapIncrease(map: String, key: String, count: Long): ZIO[Any, RedisError, Long]
  def mapDecrease(map: String, key: String, count: Long): ZIO[Any, RedisError, Long]
  def mapGet[T: JsonCodec](map: String, key: String): ZIO[Any, RedisError, Option[T]]
  def mapDelete(map: String, key: String): ZIO[Any, RedisError, Boolean]
  def mapKeys(map: String): ZIO[Any, RedisError, List[String]]
  def mapRandomKey(map: String): ZIO[Any, RedisError, Option[String]]
  def mapLen(map: String): ZIO[Any, RedisError, Long]
}
