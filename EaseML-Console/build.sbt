name := """EaseML-Console"""
organization := "LiteML"

version := "0.0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

resolvers += Resolver.mavenLocal

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
libraryDependencies += "LiteML" % "EaseML-Common" % "0.0.1"
libraryDependencies += "LiteML" % "EaseML-Dag" % "0.0.1"
libraryDependencies += "commons-logging" % "commons-logging" % "1.1.1"
// Adds additional packages into Twirl
//TwirlKeys.templateImports += "LiteML.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "LiteML.binders._"
