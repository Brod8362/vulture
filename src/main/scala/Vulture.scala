package pw.byakuren.redditmonitor

import jsonmodels.vultureconfig.VultureConfig
import net.dean.jraw.RedditClient
import net.dean.jraw.http.{OkHttpNetworkAdapter, UserAgent}
import net.dean.jraw.oauth.{Credentials, OAuthHelper}
import play.api.libs.json.Json
import pw.byakuren.redditmonitor.auth.EasyRedditOAuth

import java.io.{File, FileInputStream, FileOutputStream}
import scala.concurrent.{Await, Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}


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

  val userAgent = new UserAgent("Vulture", "pw.byakuren.redditmonitor", "v0.1", config.userName)
  val networkAdapter = new OkHttpNetworkAdapter(userAgent)
  val credentials = Credentials.installedApp("Bjbe1Yeh16iPxA", s"http://localhost:58497/redditAuth")

  //Before showing the user the URL, the HTTP server needs to be running to listen for the redirect.
  val helper = OAuthHelper.interactive(networkAdapter, credentials)
  val authUrl = helper.getAuthorizationUrl(true, false, "save", "vote", "read", "privatemessages")

  val redditClientFuture = EasyRedditOAuth.authenticate(helper)

  println(authUrl)

  Await.ready(redditClientFuture, Duration.Inf)
  redditClientFuture onComplete {
    case Success(clientOption) =>
      clientOption match {
        case Some(client) =>
          println("entering execution loop")
          executionLoop(client)
        case None =>
          println(s"big ouchie")
      }
    case Failure(t) =>
      println(s"Error occurred when authenticating. ($t)")
  }

  def executionLoop(client: RedditClient): Unit = {
    println(client)
  }

}
