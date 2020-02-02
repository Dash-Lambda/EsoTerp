ThisBuild / scalaVersion     := "2.13.0"
ThisBuild / version          := "0.2.0"
ThisBuild / organization     := "com.github.dashlambda"
ThisBuild / organizationName := "dashlambda"

lazy val root = (project in file("."))
  .settings(
    name := "Eso",
    logBuffered in Test := false,
    parallelExecution in Test := false,
    scalacOptions ++= Seq("-deprecation", "-feature"),
    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oF"),
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases")),
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0",
      "org.scala-lang" % "scala-compiler" % "2.13.0",
      "org.typelevel" %% "spire" % "0.17.0-M1",
      "org.typelevel" %% "jawn-parser" % "0.14.2",
      "org.typelevel" %% "jawn-ast" % "0.14.2",
      "org.scalactic" %% "scalactic" % "3.1.0",
      "org.scalatest" %% "scalatest" % "3.1.0" % "test"))