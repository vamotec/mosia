package app.mosia.infra.helpers.url

import zio.http.{ Response, URL }
import zio.{ Task, UIO }

import java.net.URI

trait URLHelper {
  def init(): Task[Unit]
  def stringify(query: (String, String)): Task[String]
  def getBaseUrl: UIO[String]
  def addSimpleQuery(
    urlStr: String,
    key: String,
    value: String,
    escape: Boolean = true
  ): Task[String]
  def url(path: String, query: Option[Map[String, String]]): Task[URL]
  def link(path: String, query: Option[Map[String, String]] = None): Task[String]
  def safeRedirect(response: Response, to: String): Task[Response]
  def verify(url: String): Task[Boolean]
  def registerListeners(): UIO[Unit]
}
