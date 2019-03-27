import java.io.FileInputStream
import java.nio.file.Files
import java.util.zip.GZIPInputStream

import scala.collection.GenSeq

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import sbt.Keys._
import sbt.io.{IO, Using}
import sbt.librarymanagement._
import sbt.librarymanagement.ivy._
import sbt.{Binary, Def, File}

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

  def unpackTarGz(archive: File, targetDir: File): Unit = {
    val fi = new FileInputStream(archive)
    try {
      val gi = new GZIPInputStream(fi)
      val tar = new TarArchiveInputStream(gi)

      var entry = tar.getNextTarEntry()
      while (entry != null) {
        val file = new File(targetDir + "/" + entry.getName())
        if (entry.isDirectory()) {
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

  /**
    *
    * @param moduleID
    * @param scalaBinVersion We hard-code the default, to avoid to make this def a sbt setting.
    * @return
    */
  def artifactNameSuffix(moduleID: ModuleID, scalaBinVersion: String = "2.12"): String = moduleID.crossVersion match {
    case b: Binary => s"_${b.prefix}${scalaBinVersion}${b.suffix}"
    case c: Constant => s"_${c.value}"
    case _ => ""
  }

}
