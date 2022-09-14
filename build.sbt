
name := "Search_of_article"

version := "0.1"

scalaVersion := "2.13.8"

val circeVersion = "0.14.2"
val doobieVersion = "1.0.0-M5"
val liquibase = "4.9.0"
val http4sVersion = "0.23.12"
val tapirVersion = "1.0.3"
val pureConfigVersion = "0.17.1"
val catsEffectVersion = "3.3.14"
val chimneyVersion = "0.6.2"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.typelevel" %% "cats-effect" % catsEffectVersion,

  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "org.tpolecat" %% "doobie-hikari" % doobieVersion,

  "org.liquibase" % "liquibase-core" % liquibase,

  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,

  "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-client" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,

  "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime,
  "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
  "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion,

  "io.scalaland" %% "chimney" % chimneyVersion
)