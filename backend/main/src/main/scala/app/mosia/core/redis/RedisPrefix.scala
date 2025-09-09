package app.mosia.core.redis

final case class RedisPrefix(value: String) {
  def apply(key: String): String = s"$value$key"
}
