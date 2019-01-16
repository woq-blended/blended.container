
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Build assembly artifacts (zip,gz,tgz)
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.9")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")

// Support for artifact signing with pgp
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")

//addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.9.4")
// instead we use a binary located uder project/lib containing
// https://github.com/sbt/sbt-osgi/pull/61
// but we also need to add transitive deps
libraryDependencies ++= Seq(
  "biz.aQute.bnd" % "biz.aQute.bndlib" % "4.0.0"
)

// Scala source code formatter (also used by Scala-IDE/Eclipse)
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")

// Generate logging config for test execution
addSbtPlugin("de.wayofquality" % "sbt-testlogconfig" % "0.1.0-SNAPSHOT")

// Filter resources (like Maven)
addSbtPlugin("de.wayofquality" % "sbt-filterresources" % "0.1.1-SNAPSHOT")

// Contains project dependency information from blended project as sbt plugin
//libraryDependencies ++= Seq(
addSbtPlugin(
  "de.wayofquality.blended" % "blended.dependencies" % "3.1-SNAPSHOT"
)

// We use this to materialize profiles to containers
//libraryDependencies ++= Seq(
//  "de.wayofquality.blended" %% "blended.updater.tools" % "3.1-SNAPSHOT",
//  "com.typesafe" % "config" % "1.3.3"
//)
