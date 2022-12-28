name := "com-softinio-pat"

version := "1.0"

scalaVersion := "2.13.10"

lazy val akkaVersion = "2.6.10"
lazy val zioActorsVersion = "0.1.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed"  % akkaVersion,
  "ch.qos.logback"     % "logback-classic"   % "1.2.3",
  "commons-validator"  % "commons-validator" % "1.7",
  "dev.zio"           %% "zio-actors"        % zioActorsVersion,
  "dev.zio"           %% "zio-test"          % "2.0.5" % Test
)
