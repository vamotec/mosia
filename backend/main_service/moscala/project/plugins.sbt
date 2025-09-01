addSbtPlugin("io.spray"              % "sbt-revolver"        % "0.10.0")
addSbtPlugin("ch.epfl.scala"         % "sbt-scalafix"        % "0.10.4")
addSbtPlugin("org.scalameta"         % "sbt-scalafmt"        % "2.5.0")
addSbtPlugin("com.github.sbt"        % "sbt-native-packager" % "1.9.16")
addSbtPlugin("dev.zio"               % "zio-sbt-ecosystem"   % "0.4.0-alpha.8")
addSbtPlugin("io.github.davidmweber" % "flyway-sbt"          % "7.4.0")
addSbtPlugin("com.github.ghostdogpr" % "caliban-codegen-sbt" % "2.10.0")
addSbtPlugin("com.eed3si9n"          % "sbt-buildinfo"       % "0.13.1")
addSbtPlugin("com.eed3si9n"          % "sbt-assembly"        % "2.3.1")
addSbtPlugin("com.thesamet"          % "sbt-protoc"          % "1.0.8")

libraryDependencies ++= Seq(
  "org.postgresql"                 % "postgresql"       % "42.7.7",
  "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % "0.6.3"
)
