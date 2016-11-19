name := """webcomics"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "com.lihaoyi" %% "scalatags" % "0.6.1",
  "net.ruippeixotog" %% "scala-scraper" % "1.1.0"
)

