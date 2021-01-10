name := "vulture"

version := "0.1"

scalaVersion := "2.13.4"

idePackagePrefix := Some("pw.byakuren.redditmonitor")

resolvers += "jcenter-bintray" at "https://jcenter.bintray.com"

libraryDependencies ++= Seq("net.dean.jraw" % "JRAW" % "1.1.0",
  "com.typesafe.play" %% "play-json" % "2.9.2")

enablePlugins(SbtJsonPlugin)

//sbt-json settings

jsonInterpreter := plainCaseClasses.withPlayJsonFormats

jsonOptionals += OptionalField("vultureConfig", "Watchers", "titleRegex")
jsonOptionals += OptionalField("vultureConfig", "Watchers", "contentRegex")
jsonOptionals += OptionalField("vultureConfig", "Watchers", "checkInterval")
jsonOptionals += OptionalField("vultureConfig", "Watchers", "matchEither")