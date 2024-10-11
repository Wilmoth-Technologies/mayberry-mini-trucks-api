lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "mayberry-mini-trucks-api",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.13.14",
    libraryDependencies ++= Seq(
      guice,
      "com.auth0" % "java-jwt" % "4.2.1", // For JWT token handling
      "com.typesafe.play" %% "play-json" % "2.9.2",  // Play JSON for working with JSON
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
    ),
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.0",
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.10.0",
    scalacOptions ++= Seq(
      "-feature",
      "-Werror"
    ),
    // Needed for ssl-config to create self signed certificated under Java 17
    Test / javaOptions ++= List("--add-exports=java.base/sun.security.x509=ALL-UNNAMED"),
  )