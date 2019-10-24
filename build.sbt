ThisBuild / organization := "uniud.distribuiti"
ThisBuild / version      := "0.1.0"
val akkaVersion = "2.5.25"

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

lazy val root = (project in file("."))
  .settings(scalaSettings: _*)
  .settings(dockerSettings: _*)
  .settings(
    organization := "uniud.distribuiti",
    name := "lastmile-library",
    version := "0.1.0",
    assemblyJarName := "lastmile-library:" + version.value + ".jar",
    mainClass in Compile := Some("uniud.distribuiti.lastmile.LastMile"),
    libraryDependencies ++= Seq(
      // AKKA
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % "1.0.3",
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.3",
      "junit" % "junit" % "4.12" % Test,
    )
  )
  .enablePlugins(sbtdocker.DockerPlugin)