import phoenix.ProjectFactory
import sbt._
import sbt.Keys._
import sbt.io.Using
import blended.sbt.dockercontainer.BlendedDockerContainerPlugin.{autoImport => DC}
import blended.sbt.container.BlendedContainerPlugin.{autoImport => BC}

object BlendedDockerDemoApacheds extends ProjectFactory {

  object config extends BlendedDockerContainer {
    override val projectName = "blended.docker.demo.apacheds"
    override val imageTag = "atooni/blended_apacheds"
    override val publish = false
    override val projectDir = Some("docker/blended.docker.demo.apacheds")
    override val ports = List(10389)
    override val folder = "apacheds"

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(

      DC.containerImage := {
        val file = target.value / "apacheds-2.0.0-M24-x86_64.rpm"
        // todo only download when missing
        Using.urlInputStream(
          new URL("https://archive.apache.org/dist/directory/apacheds/dist/2.0.0-M24/" + file.getName())
        ) { in => IO.transfer(in, file) }

        s"apacheds-2.0.0-M24-x86_64" -> file
      },

      DC.generateDockerfile := {
        val dockerfile = DC.dockerDir.value / "Dockerfile"

        IO.copyDirectory((Compile / sourceDirectory).value / "docker" / folder, DC.dockerDir.value)
        IO.transfer(DC.containerImage.value._2, DC.dockerDir.value / DC.containerImage.value._2.getName())

        val dockerconf = Seq(
          "FROM atooni/blended-base:latest",
          s"MAINTAINER Blended Team version: ${Blended.blendedVersion}",
          s"RUN mkdir -p /opt/${folder}",
          "ENV JAVA_HOME /opt/java",
          s"ADD files /opt/${folder}",
          "RUN yum install -y -q openldap-clients gettext vim",
          s"ADD ${DC.containerImage.value._2.getName()} /tmp",
          s"RUN yum install -y -q /tmp/${DC.containerImage.value._2.getName()}",
          s"""ENTRYPOINT ["/bin/bash", "-l", "/opt/${folder}/scripts/start.sh"]"""
        ) ++
          ports.map(p => s"EXPOSE ${p}")

        IO.write(dockerfile, dockerconf.mkString("\n"))

        dockerfile
      }
    )
  }
}
