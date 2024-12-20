//TODO: Downgrade to scala 2.12 and update play versions. We are running into Guice issues atm...
val jacksonVersion = "2.14.0"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "mayberry-mini-trucks-api",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.12.10",
    libraryDependencies ++= Seq(
      ws, specs2 % Test, guice, openId,
      "com.auth0" % "java-jwt" % "4.2.1", // For JWT token handling
      "ai.x" %% "play-json-extensions" % "0.42.0",
      "com.typesafe.play" %% "play-json" % "2.8.1",  // Play JSON for working with JSON
      "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.13.4.2",
      "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
      "com.fasterxml.jackson.jaxrs" % "jackson-jaxrs-json-provider" % jacksonVersion,
      "com.google.cloud" % "google-cloud-storage" % "2.1.4",
      "com.google.cloud" % "google-cloud-secretmanager" % "2.3.0",
      "com.azure" % "azure-cosmos" % "4.40.0",
      "io.projectreactor.netty" % "reactor-netty-http" % "1.1.1",
      "com.typesafe.play" %% "filters-helpers" % "2.8.8",
      "com.sendgrid" % "sendgrid-java" % "4.7.1",
      "org.imgscalr" % "imgscalr-lib" % "4.2", // Image compression library
),
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.3",
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.17.2",
//    scalacOptions ++= Seq(
//      "-feature",
//      "-Werror"
//    ),
    // Needed for ssl-config to create self signed certificated under Java 17
    Test / javaOptions ++= List("--add-exports=java.base/sun.security.x509=ALL-UNNAMED"),
  )