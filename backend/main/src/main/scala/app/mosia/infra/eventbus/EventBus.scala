package app.mosia.infra.eventbus

import zio.stream.ZStream
import zio.json.*
import zio.*
import zio.kafka.consumer.Consumer

trait EventBus:
  def emit[E: JsonEncoder: Tag](topic: String, payload: E): Task[Unit]
  def onEvent[E: JsonDecoder: Tag](f: E => Task[Unit]): UIO[Fiber.Runtime[Throwable, Unit]]
  def broadcast[E: JsonEncoder: Tag](topic: String, payload: E): Task[Unit]
  def subscribe[E: JsonDecoder: Tag](topics: String*): ZStream[Any, Throwable, E]
