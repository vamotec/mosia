package app.mosia.infra.eventbus

import app.mosia.core.configs.AppConfig
import org.apache.kafka.clients.producer.ProducerRecord
import zio.*
import zio.json.*
import zio.kafka.consumer.*
import zio.kafka.consumer.Consumer.{ AutoOffsetStrategy, OffsetRetrieval }
import zio.kafka.producer.*
import zio.kafka.serde.*
import zio.stream.*

import scala.util.matching.Regex

case class EventBusImpl(
  producer: Producer,
  consumer: Consumer
) extends EventBus:

  /**
   * 点对点消息
   * 由 消费组 去均衡消费（组内只有一个消费者会拿到）。
   * 使用场景：任务队列、事件驱动、点对点消费。
   */
  override def emit[E: JsonEncoder: Tag](topic: String, payload: E): Task[Unit] =
    producer
      .produce(
        new ProducerRecord[String, String](topic, null, payload.toJson),
        Serde.string,
        Serde.string
      )
      .unit

  // 广播消息
  override def broadcast[E: JsonEncoder: Tag](topic: String, payload: E): Task[Unit] =
    producer
      .produce(
        new ProducerRecord[String, String](topic, "__broadcast__", payload.toJson),
        Serde.string,
        Serde.string
      )
      .unit

  /**
   * 订阅 Kafka topic，并返回解码后的消息流
   *
   * @param topics 要订阅的主题列表
   * @tparam E 消息类型，必须有 JsonDecoder
   * @return ZStream[Any, Throwable, E] (内部自动 commit offset)
   */
  override def subscribe[E: JsonDecoder: Tag](topics: String*): ZStream[Any, Throwable, E] =
    if (topics.isEmpty)
      ZStream.fail(new IllegalArgumentException("At least one topic must be provided"))
    else
      consumer
        .plainStream(Subscription.topics(topics.head, topics.tail: _*), Serde.string, Serde.string)
        // 解码消息
        .mapZIO { record =>
          ZIO
            .fromEither(record.record.value.fromJson[E])
            .mapError(err =>
              new RuntimeException(
                s"Failed to decode message on [${record.record.topic}] at offset ${record.offset.offset}: $err"
              )
            )
            .map(value => (record.offset, value))
        }
        // 对每条消息先返回业务数据，同时提交 offset
        .mapZIO { case (offset, value) =>
          // 异步提交 offset，不阻塞业务数据返回
          offset.commit.forkDaemon.as(value)
        }

  override def onEvent[E: JsonDecoder: Tag](f: E => Task[Unit]): UIO[Fiber.Runtime[Throwable, Unit]] =
    // fork 整个流，让它在后台运行
    consumer
      .plainStream(Subscription.pattern(".*".r), Serde.string, Serde.string)
      .mapZIO { record =>
        // 解码消息
        ZIO
          .fromEither(record.record.value.fromJson[E])
          .mapError(err =>
            new RuntimeException(
              s"Failed to decode message at offset ${record.offset.offset}: $err"
            )
          )
          .flatMap { value =>
            // 执行业务逻辑，同时异步提交 offset
            (f(value) *> record.offset.commit.forkDaemon).as(value)
          }
      }
      .runDrain
      .forkDaemon

object EventBusImpl:
  private def make: ZIO[Consumer & Producer, Nothing, EventBus] =
    for
      producer <- ZIO.service[Producer]
      consumer <- ZIO.service[Consumer]
      bus      <- ZIO.succeed(new EventBusImpl(producer, consumer))
    yield bus

  val live: ZLayer[Consumer & Producer, Nothing, EventBus] = ZLayer.fromZIO(make)
