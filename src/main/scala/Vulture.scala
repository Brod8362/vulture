package pw.byakuren.redditmonitor

import jsonmodels.vultureconfig.VultureConfig
import net.dean.jraw.http.UserAgent
import play.api.libs.json.Json

import java.io.{File, FileInputStream, FileOutputStream}


object Vulture extends App {

  private val CONFIG_FILE_PATH = "vultureConfig.json"

  def exportDefaultConfig(): Unit = {
    val resource = scala.io.Source.fromURL(getClass.getClassLoader.getResource("json/vultureConfig.json"))
    val fos = new FileOutputStream(new File(CONFIG_FILE_PATH))
    fos.write(resource.mkString.getBytes)
    fos.close()
  }

  if (!new File(CONFIG_FILE_PATH).exists) {
    exportDefaultConfig()
  }

  val configFile = new File(CONFIG_FILE_PATH)
  val config = Json.parse(new FileInputStream(configFile)).as[VultureConfig]

  val userAgent = new UserAgent("bot", "pw.byakuren.redditmonitor", "v0.1", config.userName)

}
