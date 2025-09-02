import sbt.*
import sbt.Keys.*

import scala.io.Source
import scala.util.Try

object Dependencies {
  val zioVersion = "2.1.20"
  val zioHttpVersion = "3.4.0"
  val zioConfigVersion = "4.0.4"
  val calibanVersion = "2.11.1"
  val tapirVersion = "1.11.42"
  val sttpVersion = "4.0.9"
  val zioLoggingVersion = "2.5.1"
  val zioSchemaVersion = "1.7.4"
  val grpcVersion = "1.75.0"

  val zioHttp = "dev.zio" %% "zio-http" % zioHttpVersion
  val zioJson = "dev.zio" %% "zio-json" % "0.7.44"
  val zioSchema = "dev.zio" %% "zio-schema" % zioSchemaVersion
  val zioSchemaPro = "dev.zio" %% "zio-schema-protobuf" % zioSchemaVersion
  val zioSchemaJson = "dev.zio" %% "zio-schema-json" % zioSchemaVersion
  val zioLogging = "dev.zio" %% "zio-logging" % zioLoggingVersion
  val zioSlf4j = "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion
  val zingRpc = "io.grpc" % "grpc-netty" % grpcVersion

  val zioTest = "dev.zio" %% "zio-test" % zioVersion % Test
  val zioTestSBT = "dev.zio" %% "zio-test-sbt" % zioVersion % Test
  val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % zioVersion % Test

  val zioJwt = "com.github.jwt-scala" %% "jwt-zio-json" % "11.0.2"
  val zioConfig = "dev.zio" %% "zio-config" % zioConfigVersion
  val zioConfigTypesafe = "dev.zio" %% "zio-config-typesafe" % zioConfigVersion
  val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % zioConfigVersion
  val calibanCore = "com.github.ghostdogpr" %% "caliban" % calibanVersion
  val calibanTapir = "com.github.ghostdogpr" %% "caliban-tapir" % calibanVersion
  val calibanZioHttp =
    "com.github.ghostdogpr" %% "caliban-zio-http" % calibanVersion
  val quillZio = "io.getquill" %% "quill-jdbc-zio" % "4.8.6"
  val postgres = "org.postgresql" % "postgresql" % "42.7.7"
  // for autoDataMeta
  val tika = "org.apache.tika" % "tika-core" % "3.2.1"
  val log = "ch.qos.logback" % "logback-classic" % "1.5.18"
  val tapir = "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion
  val tapirZio = "com.softwaremill.sttp.tapir" %% "tapir-zio" % tapirVersion
  val tapirZioHttp =
    "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % tapirVersion
  val tapirSwagger =
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion
  val tapirZioJson =
    "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % tapirVersion
  val sttpClient = "com.softwaremill.sttp.client4" %% "core" % sttpVersion
  val sttpZio = "com.softwaremill.sttp.client4" %% "zio" % sttpVersion
  val zioRedis = "dev.zio" %% "zio-redis" % "1.1.5"
  val aws = "software.amazon.awssdk" % "s3" % "2.32.14"
  val stripe = "com.stripe" % "stripe-java" % "29.4.0"
  val kafka = "dev.zio" %% "zio-kafka" % "3.0.0"
  val sjMail = "org.simplejavamail" % "simple-java-mail" % "8.12.6"
  val scalatags = "com.lihaoyi" %% "scalatags" % "0.13.1"
  val crypto = "org.bouncycastle" % "bcprov-jdk15on" % "1.70"
  val bcrypt = "org.mindrot" % "jbcrypt" % "0.4"
  val chimney = "io.scalaland" %% "chimney" % "1.8.2"
  val lombok = "org.projectlombok" % "lombok" % "1.18.38" % "provided"
  val flywayDp = "org.flywaydb" % "flyway-core" % "11.11.2"
  val flyPostgres = "org.flywaydb" % "flyway-database-postgresql" % "11.11.2"
  val gRpcScalapb =
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
}
