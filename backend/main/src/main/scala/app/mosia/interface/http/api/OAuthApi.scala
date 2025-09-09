package app.mosia.interface.http.api

import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.ServerEndpoint
import zio.Task

trait OAuthApi:
  def endpoints: List[ServerEndpoint[ZioStreams, Task]]
