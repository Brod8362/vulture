package pw.byakuren.redditmonitor


import auth.VultureHTTP

import jsonmodels.vultureconfig.VultureConfig
import net.dean.jraw.RedditClient
import net.dean.jraw.http.{OkHttpNetworkAdapter, UserAgent}
import net.dean.jraw.oauth.{Credentials, OAuthHelper}
import play.api.libs.json.Json

import java.awt.Desktop
import java.io.{File, FileInputStream, FileOutputStream}
import java.net.URL
import java.util.UUID
import java.util.logging.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


object Vulture extends App {

  private val CLIENT_ID = "Bjbe1Yeh16iPxA"
  private val REDIRECT_URL = "http://localhost:58497/redditAuth"

  private val CURRENT_CONFIG_VERSION = 1

  private val CONFIG_FILE_PATH: String = args.find(_.matches("--config=.*")) match {
    case Some(cfgStr) =>
      cfgStr.split("=").last
    case None =>
      "vultureConfig.json"
  }

  val logger = Logger.getLogger("Vulture MainThread")

  def exportDefaultConfig(filePath: String = CONFIG_FILE_PATH): Unit = {
    logger.info(s"Exporting default config to $filePath")
    val resource = scala.io.Source.fromURL(getClass.getClassLoader.getResource("default/sensibleDefaults.json"))
    val fos = new FileOutputStream(new File(filePath))
    fos.write(resource.mkString.getBytes)
    fos.close()
  }

  args.find(_.matches("--exportDefaults.*")) match {
    case Some(str) if str.indexOf("=") != -1 =>
      exportDefaultConfig(str.split("=").last)
      System.exit(0)
    case Some(str) if str.indexOf("=") == -1 =>
      exportDefaultConfig()
      System.exit(0)
    case _ =>
  }

  if (!new File(CONFIG_FILE_PATH).exists) {
    logger.info("Config file missing")
    exportDefaultConfig()
  }

  val configFile = new File(CONFIG_FILE_PATH)
  implicit val config: VultureConfig = Json.parse(new FileInputStream(configFile)).as[VultureConfig]
  logger.info(s"Loaded config file $CONFIG_FILE_PATH")

  if (config.configVersion < CURRENT_CONFIG_VERSION) {
    throw new RuntimeException(s"Your config file is version ${config.configVersion.toInt}," +
      s" but the currently supported version is $CURRENT_CONFIG_VERSION. Please check the github page for how to update.")
  }

  implicit val authMode: AuthMode.Value = if (args.contains("--userless") || config.authMode =="userless") {
    AuthMode.Userless
  } else if (config.authMode =="user") {
    AuthMode.User
  } else {
    throw new UnsupportedOperationException(s"auth mode ${config.authMode} is unsupported")
  }

  val userAgent = new UserAgent("Vulture", "pw.byakuren.redditmonitor", "v0.1", "brod8362")
  val networkAdapter = new OkHttpNetworkAdapter(userAgent)
  val credentials: Credentials = authMode match {
    case AuthMode.User =>
      logger.info("Running user mode")
      Credentials.installedApp(CLIENT_ID, REDIRECT_URL)
    case AuthMode.Userless =>
      logger.info("Running in userless mode")
      Credentials.userlessApp(CLIENT_ID, UUID.randomUUID())
  }

  val http = new VultureHTTP(() => { stop() })

  val redditClientFuture: Future[Option[RedditClient]] = authMode match {
    case AuthMode.User =>
      //Before showing the user the URL, the HTTP server needs to be running to listen for the redirect.
      val helper = OAuthHelper.interactive(networkAdapter, credentials)
      //https://www.reddit.com/dev/api/oauth/ for a list of all scopes
      val authUrl = helper.getAuthorizationUrl(true, false, "save", "vote", "read", "privatemessages", "identity")
      Option(Desktop.getDesktop) match {
        case Some(desktop) if desktop.isSupported(Desktop.Action.BROWSE) && !args.contains("--noBrowser") =>
          desktop.browse(new URL(authUrl).toURI)
        case _ =>
          println(s"Click here to authenticate: $authUrl")
      }
      http.authenticate(helper)
    case AuthMode.Userless =>
      println(s"Status and monitoring available at http://localhost:58497/status")
      Future.successful(Some(OAuthHelper.automatic(networkAdapter, credentials)))
  }

  //TODO store token or w/e so you dont have to authenticate every time

  redditClientFuture onComplete {
    case Success(clientOption) =>
      clientOption match {
        case Some(client) =>
          implicit val _client: RedditClient = client
          logger.info("Entering execution loop")
          val vultureClient = new VultureClient
          http.vultureClientOption = Some(vultureClient)
          vultureClient.run()
        case None =>
          logger.severe("big ouchie")
      }
    case Failure(t) =>
      logger.severe(s"Error occurred when authenticating. ($t)")
      t.printStackTrace()
  }

  def stop(): Unit = {
    logger.info("Exit requested from HTTP server")
    System.exit(0)
  }

}
