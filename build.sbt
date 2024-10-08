lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "mayberry-mini-trucks-api",
    version := "1.0-SNAPSHOT",
    crossScalaVersions := Seq("2.13.14", "3.3.3"),
    scalaVersion := crossScalaVersions.value.head,
    libraryDependencies ++= Seq(
      guice,
      "com.github.jwt-scala" %% "jwt-play-json" % "10.0.1",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-Werror"
    ),
    // Needed for ssl-config to create self signed certificated under Java 17
    Test / javaOptions ++= List("--add-exports=java.base/sun.security.x509=ALL-UNNAMED"),
  )