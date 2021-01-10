package pw.byakuren.redditmonitor


import jsonmodels.vultureconfig.VultureConfig
import net.dean.jraw.RedditClient
import net.dean.jraw.http.{OkHttpNetworkAdapter, UserAgent}
import net.dean.jraw.oauth.{Credentials, OAuthHelper}
import play.api.libs.json.Json
import pw.byakuren.redditmonitor.auth.EasyRedditOAuth

import java.io.{File, FileInputStream, FileOutputStream}
import java.util.logging.Logger
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}


object Vulture extends App {

  private val CONFIG_FILE_PATH = "vultureConfig.json"

  val logger = Logger.getLogger("Vulture")

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
  implicit val config: VultureConfig = Json.parse(new FileInputStream(configFile)).as[VultureConfig]

  val userAgent = new UserAgent("Vulture", "pw.byakuren.redditmonitor", "v0.1", "brod8362")
  val networkAdapter = new OkHttpNetworkAdapter(userAgent)
  val credentials = Credentials.installedApp("Bjbe1Yeh16iPxA", s"http://localhost:58497/redditAuth")

  //Before showing the user the URL, the HTTP server needs to be running to listen for the redirect.
  val helper = OAuthHelper.interactive(networkAdapter, credentials)
  //https://www.reddit.com/dev/api/oauth/ for a list of all scopes
  val authUrl = helper.getAuthorizationUrl(true, false, "save", "vote", "read", "privatemessages", "identity")

  val redditClientFuture: Future[Option[RedditClient]] = EasyRedditOAuth.authenticate(helper)
  //TODO store token or w/e so you dont have to authenticate every time

  println(authUrl)

  Await.ready(redditClientFuture, Duration.Inf)
  redditClientFuture onComplete {
    case Success(clientOption) =>
      clientOption match {
        case Some(client) =>
          println("entering execution loop")
          val vultureClient = new VultureClient(client)
          vultureClient.run()
        case None =>
          println(s"big ouchie")
      }
    case Failure(t) =>
      println(s"Error occurred when authenticating. ($t)")
  }

}
