package app.mosia.infra.cache

trait CacheProvider:
  def session: CacheNamespace
  def cache: CacheNamespace
  def event: CacheNamespace
