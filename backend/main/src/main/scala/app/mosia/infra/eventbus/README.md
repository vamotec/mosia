          ┌────────────────────────────┐
          │        Kafka Broker        │
          │  ┌───────────────┐         │
          │  │ mail-topic    │         │
          │  │ retry-topic   │         │
          │  │ dead-letter   │         │
          │  └───────────────┘         │
          └─────────────▲──────────────┘
                        │
                        │ 消费消息
                        │
             ┌──────────┴───────────┐
             │  KafkaSubscriber      │
             │  subscribe[E](topics)│
             │  - 订阅 Kafka topic  │
             │  - JSON 解码 E       │
             │  - 自动提交 offset    │
             └──────────┬───────────┘
                        │ ZStream[E]
                        │
        ┌───────────────┴──────────────────────────┐
        │      上层业务处理逻辑                       │
        │   (例如邮件处理器)                          │
        │  - mailerService.sendEmailInternal(mail) │
        │  - 失败处理 / 重试 / dead-letter           │
        └──────────────────────────────────────────┘