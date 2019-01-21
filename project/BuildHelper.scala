import java.io.FileInputStream
import java.nio.file.Files
import java.util.zip.GZIPInputStream

import scala.collection.GenSeq

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import sbt.Keys._
import sbt.io.{IO, Using}
import sbt.librarymanagement._
import sbt.librarymanagement.ivy._
import sbt.{Def, File}

object BuildHelper {

  private[this] val log = sbt.util.LogExchange.logger("blended")
  private[this] val ivyConfig = InlineIvyConfiguration().withLog(log)
  private[this] val resolver = IvyDependencyResolution(ivyConfig)

  def deleteRecursive(f: File): Unit = {
    if (f.isDirectory()) {
      f.listFiles().foreach(deleteRecursive)
    }
    f.delete()
  }

  def resolveModuleFile(mid: ModuleID, targetPath: File): Vector[File] = {

    resolver.retrieve(mid, None, targetPath, log) match {
      case Left(w) => throw w.resolveException
      case Right(files) => files
    }
  }

  def resolveModuleFile(mid: ModuleID, scalaModuleInfo: Option[ScalaModuleInfo], targetPath: File): Vector[File] = {

    resolver.retrieve(mid, scalaModuleInfo, targetPath, log) match {
      case Left(w) => throw w.resolveException
      case Right(files) => files
    }
  }

  def readVersion(versionFile: File): Seq[Def.Setting[_]] = {
    val buildVersion = Files.readAllLines(versionFile.toPath()).get(0)

    Seq(
      version := buildVersion,
      isSnapshot := buildVersion.endsWith("SNAPSHOT")
    )
  }

  def readAsVersion(versionFile: File): String = {
    Files.readAllLines(versionFile.toPath()).get(0).trim()
  }

  def unpackTarGz(archive: File, targetDir: File): Unit = {
    val fi = new FileInputStream(archive)
    try {
      val gi = new GZIPInputStream(fi)
      val tar = new TarArchiveInputStream(gi)

      var entry = tar.getNextTarEntry()
      while(entry != null) {
        val file = new File(targetDir + "/" + entry.getName())
        if(entry.isDirectory()) {
          file.mkdirs()
        } else {
          val size = entry.getSize().intValue()
          val offset = 0
          val content = new Array[Byte](size)
          tar.read(content, offset, content.length - offset)
          IO.write(file, content)
        }

        // next while-iteration
        entry = tar.getNextTarEntry()
      }

      tar.close()

    } finally {
      fi.close()
    }
  }

}
