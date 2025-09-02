package app.mosia.core.kafka

import app.mosia.core.configs.AppConfig
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.kafka.producer.{Producer, ProducerSettings}
// 创建统一的 Kafka 配置层
object KafkaLayers {
  def producerLayer: ZLayer[Ref[AppConfig], Throwable, Producer] =
    ZLayer.scoped {
      for {
        configRef <- ZIO.service[Ref[AppConfig]]
        config    <- configRef.get
        producer  <- Producer.make(
                       ProducerSettings(config.kafka.bootstrapServers)
                         .withProperty("security.protocol", "PLAINTEXT")
                         .withProperty("acks", "all")    // 确保消息被完全复制
                         .withProperty("retries", "3")
                         .withProperty("batch.size", "16384")
                         .withProperty("linger.ms", "5") // 批量发送延迟
                         .withProperty("buffer.memory", "33554432")
                         .withProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
                         .withProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
                     )
      } yield producer
    }

  def consumerLayer: ZLayer[Ref[AppConfig], Throwable, Consumer] =
    ZLayer.scoped {
      for {
        configRef <- ZIO.service[Ref[AppConfig]]
        config    <- configRef.get
        consumer  <- Consumer.make(
                       ConsumerSettings(config.kafka.bootstrapServers)
                         .withGroupId("default-group")
                         .withProperty("security.protocol", "PLAINTEXT")
                         .withProperty("enable.auto.commit", "false")
                         .withProperty("auto.offset.reset", "latest")
                         .withProperty("max.poll.records", "10")
                         .withProperty("session.timeout.ms", "30000")
                         .withProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
                         .withProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
                         .withOffsetRetrieval(OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest))
                     )
      } yield consumer
    }
}
