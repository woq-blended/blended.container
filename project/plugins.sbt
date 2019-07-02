
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
addSbtPlugin("de.wayofquality.sbt" % "sbt-testlogconfig" % "0.1.0")

// Filter resources (like Maven)
addSbtPlugin("de.wayofquality.sbt" % "sbt-filterresources" % "0.1.2")

// Contains project dependency information from blended project as sbt plugin
//libraryDependencies ++= Seq(
addSbtPlugin(
  "de.wayofquality.blended" % "blended.dependencies" % "3.1.ui-SNAPSHOT"
)

addSbtPlugin("de.wayofquality.sbt" % "sbt-phoenix" % "0.1.0")

addSbtPlugin("de.wayofquality.blended" % "sbt-blendedbuild" % "0.1.2")
