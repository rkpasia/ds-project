lazy val scalaSettings = Seq(
  scalaVersion := "2.13.0",
  scalacOptions ++= Seq(
    "-deprecation",
    "-Ywarn-value-discard",
    "-Xfatal-warnings"
  )
)

lazy val dockerSettings = Seq(
  // https://github.com/marcuslonnberg/sbt-docker
  dockerfile in docker := {
    // The assembly task generates a fat JAR file
    val artifact: File = assembly.value
    // val artifactTargetPath = s"/app/${artifact.name}"
    // use fixed artifact path to avoid fails on version change when using custom docker command
    val artifactTargetPath = s"/app/${name.value}.jar"

    new Dockerfile {
      from("openjdk:14")
      add(artifact, artifactTargetPath)
      expose(8080)
      expose(8558)
      expose(2552)
      entryPoint("java")
      cmd("-jar", artifactTargetPath)
    }
  },
  imageNames in docker := Seq(
    ImageName(s"lastmile/${name.value}:latest"),
    ImageName(
      namespace = Some("lastmile"),
      repository = name.value,
      tag = Some(version.value)
    )
  )
)

val akkaVersion = "2.5.25"

lazy val passenger = (project in file("."))
  .settings(dockerSettings: _*)
  .settings(scalaSettings: _*)
  .settings(
    organization := "uniud.distribuiti",
    name := "passenger-app",
    version := "0.1.0",
    mainClass in assembly := Some("uniud.distribuiti.client.Passenger"),
    libraryDependencies ++= Seq(
      "uniud.distribuiti" % "lastmile-library_2.13" % "0.1.0"
    )
  )
  .enablePlugins(sbtdocker.DockerPlugin)