import sbt._

class AkkaAutoscale(info: ProjectInfo) extends DefaultProject(info) with AkkaProject{

        val scala_tools_releases = "scala-tools.releases" at "http://scala-tools.org/repo-releases"
        val scala_tools_snapshots = "scala-tools.snapshots" at "http://scala-tools.org/repo-snapshots"

        val sonatypeNexusSnapshots = "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
        val sonatypeNexusReleases = "Sonatype Nexus Releases" at "https://oss.sonatype.org/content/repositories/releases"

	val specs_2_8_0 = "org.scala-tools.testing" % "specs_2.8.0" % "1.6.5" % "test"

	override def mainClass = Some("com.vasilrem.akka.easyscale.boot.Run")

}