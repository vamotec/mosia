import com.github.tototoshi.sbt.slick.CodegenPlugin.autoImport.*
import sbt.*
import sbt.Keys.*
import slick.codegen.SourceCodeGenerator
import slick.model as m

import scala.io.Source
import scala.util.Try

object Dependencies {
  lazy val databaseUrl: String      = sys.env.getOrElse("DB_DEFAULT_URL", "DB_URL is not set")
  lazy val databaseUser: String     = sys.env.getOrElse("DB_DEFAULT_USER", "DB_DEFAULT_USER is not set")
  lazy val databasePassword: String = sys.env.getOrElse("DB_DEFAULT_PASSWORD", "DB_DEFAULT_PASSWORD is not set")

  def settingsCodegen = Seq(
    slickCodegenDatabaseUrl           := databaseUrl,
    slickCodegenDatabaseUser          := databaseUser,
    slickCodegenDatabasePassword      := databasePassword,
    slickCodegenDriver                := slick.jdbc.PostgresProfile,
    slickCodegenJdbcDriver            := "org.postgresql.Driver",
    slickCodegenOutputPackage         := "app.mosia.models",
    slickCodegenExcludedTables        := Seq("schema_version"),
    slickCodegenCodeGenerator         := { (model: m.Model) =>
      new SourceCodeGenerator(model) {
        override def writeStringToFile(content: String, folder: String, pkg: String, fileName: String): Unit =
          // 遍历所有表
          for (table <- model.tables) {
            val tableName          = table.name.table
            val processedTableName = tableName.split('_').map(_.capitalize).mkString("")
            val newFileName        = s"Db$processedTableName.scala" // 使用表名生成文件名
            val tableContent       = generateCode(table, pkg)       // 为每张表生成代码

            // 调用父类方法写入文件
            super[SourceCodeGenerator].writeStringToFile(tableContent, folder, pkg, newFileName)
          }

        private def generateCode(table: m.Table, pkg: String): String = {
          val tableName          = table.name.table
          val processedTableName = tableName.split('_').map(_.capitalize).mkString("")
          // 生成字段代码，包括键属性的注释
          val fields             = table.columns.map { column =>
            val columnName          = column.name
            val processedColumnName =
              if (columnName == "type") "`type`"
              else
                columnName
                  .split('_')
                  .zipWithIndex
                  .map { case (part, index) =>
                    if (index == 0) part // 首部分保持不变
                    else part.capitalize // 其余部分首字母大写
                  }
                  .mkString("")
            val columnType          = mapColumnType(column.tpe, column.nullable) // 映射类型并处理可空性
            s"  $processedColumnName: $columnType"
          }.mkString(", \n")
          // 生成完整代码
          s"""
             |// AUTO-GENERATED Quill data model
             |package $pkg
             |
             |import app.mosia.core.types.JSONValue
             |import io.getquill.*
             |import zio.json.*
             |
             |case class Db$processedTableName(
             |$fields
             |)
             |
             |object Db$processedTableName:
             |    inline given schema: Quoted[EntityQuery[Db$processedTableName]] = quote:
             |      querySchema[Db$processedTableName]("public.$tableName")
             |
             |    given JsonEncoder[Db$processedTableName] = DeriveJsonEncoder.gen[Db$processedTableName]
             |    given JsonDecoder[Db$processedTableName] = DeriveJsonDecoder.gen[Db$processedTableName]
          """.stripMargin
        }

        // Helper function to map database types to Scala types, with nullable handling
        private def mapColumnType(dbType: String, nullable: Boolean): String = {
          // println(s"Debug: dbType = $dbType") // 打印 dbType 的值，用于调试
          val normalizedType = dbType match {
            case "java.sql.Timestamp" => "java.time.Instant"
            case "java.sql.Blob"      => "JSONValue"
            case "Short"              => "Int"
            case _                    => dbType // 默认类型
          }
          if (nullable) s"Option[$normalizedType]" else normalizedType
        }
      }
    },
    slickCodegenOutputToMultipleFiles := true,
    Compile / sourceGenerators += slickCodegen.taskValue,
    slickCodegenOutputDir             := (Compile / sourceManaged).value
  )

  val zioVersion        = "2.1.20"
  val zioHttpVersion    = "3.4.0"
  val zioConfigVersion  = "4.0.4"
  val calibanVersion    = "2.11.1"
  val tapirVersion      = "1.11.42"
  val sttpVersion       = "4.0.9"
  val zioLoggingVersion = "2.5.1"
  val zioSchemaVersion  = "1.7.4"
  val grpcVersion       = "1.75.0"

  val zioHttp       = "dev.zio" %% "zio-http"            % zioHttpVersion
  val zioJson       = "dev.zio" %% "zio-json"            % "0.7.44"
  val zioSchema     = "dev.zio" %% "zio-schema"          % zioSchemaVersion
  val zioSchemaPro  = "dev.zio" %% "zio-schema-protobuf" % zioSchemaVersion
  val zioSchemaJson = "dev.zio" %% "zio-schema-json"     % zioSchemaVersion
  val zioLogging    = "dev.zio" %% "zio-logging"         % zioLoggingVersion
  val zioSlf4j      = "dev.zio" %% "zio-logging-slf4j"   % zioLoggingVersion
  val zingRpc       = "io.grpc"  % "grpc-netty"          % grpcVersion

  val zioTest         = "dev.zio" %% "zio-test"          % zioVersion % Test
  val zioTestSBT      = "dev.zio" %% "zio-test-sbt"      % zioVersion % Test
  val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % zioVersion % Test

  val zioJwt            = "com.github.jwt-scala"          %% "jwt-zio-json"               % "11.0.2"
  val zioConfig         = "dev.zio"                       %% "zio-config"                 % zioConfigVersion
  val zioConfigTypesafe = "dev.zio"                       %% "zio-config-typesafe"        % zioConfigVersion
  val zioConfigMagnolia = "dev.zio"                       %% "zio-config-magnolia"        % zioConfigVersion
  val calibanCore       = "com.github.ghostdogpr"         %% "caliban"                    % calibanVersion
  val calibanTapir      = "com.github.ghostdogpr"         %% "caliban-tapir"              % calibanVersion
  val calibanZioHttp    = "com.github.ghostdogpr"         %% "caliban-zio-http"           % calibanVersion
  val quillZio          = "io.getquill"                   %% "quill-jdbc-zio"             % "4.8.6"
  val postgres          = "org.postgresql"                 % "postgresql"                 % "42.7.7"
  // for autoDataMeta
  val tika              = "org.apache.tika"                % "tika-core"                  % "3.2.1"
  val log               = "ch.qos.logback"                 % "logback-classic"            % "1.5.18"
  val tapir             = "com.softwaremill.sttp.tapir"   %% "tapir-core"                 % tapirVersion
  val tapirZio          = "com.softwaremill.sttp.tapir"   %% "tapir-zio"                  % tapirVersion
  val tapirZioHttp      = "com.softwaremill.sttp.tapir"   %% "tapir-zio-http-server"      % tapirVersion
  val tapirSwagger      = "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle"    % tapirVersion
  val tapirZioJson      = "com.softwaremill.sttp.tapir"   %% "tapir-json-zio"             % tapirVersion
  val sttpClient        = "com.softwaremill.sttp.client4" %% "core"                       % sttpVersion
  val sttpZio           = "com.softwaremill.sttp.client4" %% "zio"                        % sttpVersion
  val zioRedis          = "dev.zio"                       %% "zio-redis"                  % "1.1.5"
  val aws               = "software.amazon.awssdk"         % "s3"                         % "2.32.14"
  val stripe            = "com.stripe"                     % "stripe-java"                % "29.4.0"
  val kafka             = "dev.zio"                       %% "zio-kafka"                  % "3.0.0"
  val sjMail            = "org.simplejavamail"             % "simple-java-mail"           % "8.12.6"
  val scalatags         = "com.lihaoyi"                   %% "scalatags"                  % "0.13.1"
  val crypto            = "org.bouncycastle"               % "bcprov-jdk15on"             % "1.70"
  val bcrypt            = "org.mindrot"                    % "jbcrypt"                    % "0.4"
  val chimney           = "io.scalaland"                  %% "chimney"                    % "1.8.2"
  val lombok            = "org.projectlombok"              % "lombok"                     % "1.18.38" % "provided"
  val flywayDp          = "org.flywaydb"                   % "flyway-core"                % "11.11.2"
  val flyPostgres       = "org.flywaydb"                   % "flyway-database-postgresql" % "11.11.2"
  val gRpcScalapb       = "com.thesamet.scalapb"          %% "scalapb-runtime-grpc"       % scalapb.compiler.Version.scalapbVersion
}
