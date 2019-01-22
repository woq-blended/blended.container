import BlendedDockerContainer.{containerImage, dockerDir}
import sbt._
import sbt.Keys._
import sbt.io.Using

object BlendedDockerDemoApacheds extends ProjectFactory {

  val helper = new BlendedDockerContainer(
    projectName = "blended.docker.demo.apacheds",
    imageTag = "atooni/blended_apacheds",
    publish = false,
    projectDir = Some("docker/blended.docker.demo.apacheds"),
    ports = List(10389),
    folder = "apacheds",
    // TODO: no supported yet!
    overlays = List()
  ) {

    override def settings: Seq[sbt.Setting[_]] = super.settings ++ Seq(

      BlendedDockerContainer.containerImage := {
        val file = target.value / "apacheds-2.0.0-M24-x86_64.rpm"
        // todo only download when missing
        Using.urlInputStream(
          new URL("http://www-eu.apache.org/dist//directory/apacheds/dist/2.0.0-M24/" + file.getName())
        ) { in => IO.transfer(in, file) }

        s"apacheds-2.0.0-M24-x86_64" -> file
      },

      BlendedDockerContainer.generateDockerfile := {
        val dockerfile = dockerDir.value / "Dockerfile"

        IO.copyDirectory((Compile / sourceDirectory).value / "docker" / folder, dockerDir.value)
        IO.transfer(containerImage.value._2, dockerDir.value / containerImage.value._2.getName())

        val dockerconf = Seq(
          "FROM atooni/blended-base:latest",
          s"MAINTAINER Blended Team version: ${Blended.blendedVersion}",
          s"RUN mkdir -p /opt/${folder}",
          "ENV JAVA_HOME /opt/java",
          s"ADD files /opt/${folder}",
          "RUN yum install -y -q openldap-clients gettext vim",
          s"ADD ${containerImage.value._2.getName()} /tmp",
          s"RUN yum install -y -q /tmp/${containerImage.value._2.getName()}",
          s"""ENTRYPOINT ["/bin/bash", "-l", "/opt/${folder}/scripts/start.sh"]"""
        ) ++
          ports.map(p => s"EXPOSE ${p}")

        IO.write(dockerfile, dockerconf.mkString("\n"))

        dockerfile
      }

    )

  }

  override val project = helper.baseProject
}