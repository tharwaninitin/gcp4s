import Versions._
import sbt._

object Dependencies {

  lazy val coreLibs = List(
    "dev.zio"                %% "zio"                     % ZioVersion,
    "dev.zio"                %% "zio-streams"             % ZioVersion,
    "org.scala-lang.modules" %% "scala-collection-compat" % ScalaJavaCollectionCompat,
    "org.slf4j"               % "slf4j-api"               % Sl4jVersion
  )

  lazy val bqLibs = List(
    "com.google.cloud" % "google-cloud-bigquery" % GcpBqVersion
  )

  lazy val dpLibs = List(
    "com.google.cloud" % "google-cloud-dataproc" % GcpDpVersion
  )

  lazy val gcsLibs = List(
    "com.google.cloud" % "google-cloud-storage" % GcpGcsVersion
  )

  lazy val testLibs = List(
    "dev.zio"       %% "zio-test"        % ZioVersion,
    "dev.zio"       %% "zio-test-sbt"    % ZioVersion,
    "ch.qos.logback" % "logback-classic" % LogbackVersion
  ).map(_ % Test)

}
