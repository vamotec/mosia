import Dependencies.*
import sbtassembly.AssemblyPlugin.autoImport.*

ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.6"
ThisBuild / sbtVersion   := "1.11.4"
ThisBuild / organization := "app.mosia"
ThisBuild / name         := "moscala"
ThisBuild / fork         := true
ThisBuild / scalacOptions ++= Seq(
  "-Ydebug", // 启用调试输出
  "-Xmax-inlines:64",
  "-deprecation"
)

def settingsApp = Seq(
  name                             := "moscala",
  Compile / run / mainClass        := Option("app.mosia.Main"),
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  assembly / assemblyMergeStrategy := {
    case PathList("META-INF", "services", _*)                                         => MergeStrategy.concat
    case PathList("META-INF", "io.netty.versions.properties")                         => MergeStrategy.first
    case PathList("META-INF", "MANIFEST.MF")                                          => MergeStrategy.discard
    case PathList("META-INF", "maven", "org.webjars", "swagger-ui", "pom.properties") =>
      MergeStrategy.singleOrError
    case PathList("META-INF", "resources", "webjars", "swagger-ui", _*)               =>
      MergeStrategy.singleOrError
    case PathList("META-INF", _*)                                                     => MergeStrategy.discard
    case "reference.conf"                                                             => MergeStrategy.concat
    case "application.conf"                                                           => MergeStrategy.concat
    case "module-info.class"                                                          => MergeStrategy.discard
    case x if x.endsWith("LICENSE-2.0.txt")                                           => MergeStrategy.first
    case _                                                                            => MergeStrategy.first
  },
  Compile / PB.targets             := Seq(
    scalapb.gen(grpc = true)          -> (Compile / sourceManaged).value / "scalapb",
    scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value / "scalapb"
  ),
  libraryDependencies ++= Seq(
    zioHttp,
    zioTest,
    zioJson,
    zioTestSBT,
    zioTestMagnolia,
    zioConfig,
    zioSchema,
    zioSchemaPro,
    zioSchemaJson,
    zioConfigTypesafe,
    zioConfigMagnolia,
    zioLogging,
    zioSlf4j,
    zioJwt,
    calibanCore,
    calibanTapir,
    calibanZioHttp,
    quillZio,
    postgres,
    tapir,
    tapirZio,
    tapirZioHttp,
    tapirSwagger,
    tapirZioJson,
    sttpClient,
    sttpZio,
    zioRedis,
    aws,
    tika,
    log,
    stripe,
    kafka,
    sjMail,
    scalatags,
    crypto,
    bcrypt,
    chimney,
    lombok,
    flywayDp,
    flyPostgres,
    zingRpc,
    gRpcScalapb
  )
)

lazy val flyway = (project in file("flyway"))
  .enablePlugins(FlywayPlugin)
  .settings(
    flywayUrl       := databaseUrl,
    flywayUser      := databaseUser,
    flywayPassword  := databasePassword,
    flywayLocations := Seq("filesystem:src/main/resources/db/migration")
  )

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, CodegenPlugin, BuildInfoPlugin, AssemblyPlugin)
  .settings(settingsApp)
  .settings(settingsCodegen)

buildInfoKeys    := Seq[BuildInfoKey](
  name,
  version,
  scalaVersion,
  sbtVersion
)
buildInfoPackage := "app.mosia.build"
buildInfoObject  := "BuildInfo"

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
