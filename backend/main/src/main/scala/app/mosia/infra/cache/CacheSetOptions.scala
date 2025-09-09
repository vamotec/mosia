package app.mosia.infra.cache

import zio.Duration

/**
 * @param ttl
 *   in milliseconds
 */
case class CacheSetOptions(ttl: Option[Duration] = None)
