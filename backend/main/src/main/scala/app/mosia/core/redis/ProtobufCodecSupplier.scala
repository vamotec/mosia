package app.mosia.core.redis

import zio.redis.*
import zio.schema.*
import zio.schema.codec.*

object ProtobufCodecSupplier extends CodecSupplier {
  override def get[A: Schema]: BinaryCodec[A] = ProtobufCodec.protobufCodec
}
