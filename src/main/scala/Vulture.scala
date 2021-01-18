package pw.byakuren.redditmonitor


import auth.VultureHTTP

import jsonmodels.vultureconfig.VultureConfig
import net.dean.jraw.RedditClient
import net.dean.jraw.http.{OkHttpNetworkAdapter, UserAgent}
import net.dean.jraw.oauth.{AccountHelper, Credentials, JsonFileTokenStore, OAuthHelper}
import play.api.libs.json.Json

import java.awt.Desktop
import java.io.{File, FileInputStream, FileNotFoundException, FileOutputStream}
import java.net.{InetAddress, URL}
import java.nio.file.{Files, Paths}
import java.util.UUID
import java.util.logging.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.jdk.CollectionConverters._


object Vulture extends App {

  /*
  Pre-setup stuff.
   */
  private val CLIENT_ID = "Bjbe1Yeh16iPxA"
  private val REDIRECT_URL = "http://localhost:58497/redditAuth"

  private val CURRENT_CONFIG_VERSION = 1

  private val UUID_PATH = Paths.get(".vulture/uuid")
  private val TOKEN_PATH = Paths.get(".vulture/token")

  val logger = Logger.getLogger("Vulture MainThread")

  //Ensure the cache directory exists
  new File(".vulture/").mkdirs()

  /*
  Create token store and load data.
   */
  private val tokenStore = new JsonFileTokenStore(TOKEN_PATH.toFile)
  tokenStore.setAutoPersist(true)
  try {
    tokenStore.load()
  } catch {
    case e: FileNotFoundException =>
      logger.info("Creating token store file")
      TOKEN_PATH.toFile.createNewFile()
  }

  /*
    Fetch UUID from disk or generate a new one
   */
  private val DEVICE_UUID: UUID = if (Files.exists(UUID_PATH)) {
    logger.info("Reading existing UUID")
    UUID.fromString(Files.readString(UUID_PATH))
  } else {
    logger.info("Using new UUID")
    val tempId = UUID.randomUUID()
    val output = new FileOutputStream(UUID_PATH.toFile)
    output.write(tempId.toString.getBytes)
    output.close()
    tempId
  }

  /*
  Determine the config file path (considering command line options)

  If the option --config=path is supplied, that path will be used
  Otherwise, it will use the default path (vultureConfig.json)
   */
  private val CONFIG_FILE_PATH: String = args.find(_.matches("--config=.*")) match {
    case Some(cfgStr) =>
      cfgStr.split("=").last
    case None =>
      "vultureConfig.json"
  }

  /*
  Does what it says, though this could probably do with some reorganizing.
   */
  def exportDefaultConfig(filePath: String = CONFIG_FILE_PATH): Unit = {
    logger.info(s"Exporting default config to $filePath")
    val resource = scala.io.Source.fromURL(getClass.getClassLoader.getResource("default/sensibleDefaults.json"))
    val fos = new FileOutputStream(new File(filePath))
    fos.write(resource.mkString.getBytes)
    fos.close()
  }

  /*
  Export defaults and exit (if requested)
   */
  args.find(_.matches("--exportDefaults.*")) match {
    case Some(str) if str.indexOf("=") != -1 =>
      exportDefaultConfig(str.split("=").last)
      System.exit(0)
    case Some(str) if str.indexOf("=") == -1 =>
      exportDefaultConfig()
      System.exit(0)
    case _ =>
  }

  /*
  Generate a new config file automatically if it's missing at the current cfg dir
   */
  if (!new File(CONFIG_FILE_PATH).exists) {
    logger.info("Config file missing")
    exportDefaultConfig()
  }

  val configFile = new File(CONFIG_FILE_PATH)
  implicit val config: VultureConfig = Json.parse(new FileInputStream(configFile)).as[VultureConfig]
  logger.info(s"Loaded config file $CONFIG_FILE_PATH")

  /*
  Config version checker
  I felt this was necessary as it's a bit more of a readable error than the error that the JSON library spews out are.
  It also encourages users to go and check what's in the new version so they can properly update their config
   */
  if (config.configVersion < CURRENT_CONFIG_VERSION) {
    throw new RuntimeException(s"Your config file is version ${config.configVersion.toInt}," +
      s" but the currently supported version is $CURRENT_CONFIG_VERSION. Please check the github page for how to update.")
  }

  /*
  Determine authmode.
   */
  implicit val authMode: AuthMode.Value = if (args.contains("--userless") || config.authMode == "userless") {
    AuthMode.Userless
  } else if (config.authMode == "user") {
    AuthMode.User
  } else {
    throw new UnsupportedOperationException(s"auth mode ${config.authMode} is unsupported")
  }

  //TODO steal the git versioning from score and use it here
  //https://github.com/ScoreUnder/canti-bot/commit/4b57940da00dd87f260055868cd020bba6a1f45d
  val userAgent = new UserAgent("Vulture", "pw.byakuren.redditmonitor", "v0.1" /*here*/ , "brod8362")
  val networkAdapter = new OkHttpNetworkAdapter(userAgent)
  val credentials: Credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL)

  val acctHelper = new AccountHelper(networkAdapter, credentials, tokenStore, DEVICE_UUID)

  val httpServer = new VultureHTTP(() => {
    stop()
  })

  val userPath = Paths.get(".vulture/user")

  val redditClientFuture: Future[Option[RedditClient]] = authMode match {
    case AuthMode.User =>
      if (Files.exists(userPath)) {
        val username = Files.readString(userPath)
        println(s"Status and monitoring available at http://localhost:58497/status")
        Future.successful(Option(acctHelper.trySwitchToUser(username)))
      } else {
        //Before showing the user the URL, the HTTP server needs to be running to listen for the redirect.
        //The server should already be running at this point, unless it threw an exception
        val helper = acctHelper.switchToNewUser()
        //https://www.reddit.com/dev/api/oauth/ for a list of all scopes
        val authUrl = helper.getAuthorizationUrl(true, false, "save", "vote", "read", "privatemessages", "identity")
        Option(Desktop.getDesktop) match {
          case Some(desktop) if desktop.isSupported(Desktop.Action.BROWSE) && !args.contains("--noBrowser") =>
            desktop.browse(new URL(authUrl).toURI)
          case _ =>
            println(s"Click here to authenticate: $authUrl")
        }
        httpServer.authenticate(helper)
      }
    case AuthMode.Userless =>
      println(s"Status and monitoring available at http://localhost:58497/status")
      Future.successful(Some(acctHelper.switchToUserless()))
  }

  redditClientFuture onComplete {
    case Success(clientOption) =>
      clientOption match {
        case Some(client) =>
          implicit val _client: RedditClient = client
          if (!Files.exists(userPath)) {
            try {
              val un = client.requireAuthenticatedUser()
              val fos = new FileOutputStream(userPath.toFile)
              fos.write(un.getBytes)
              fos.close()
              logger.info(s"Saved username '$un' to disk")
            } catch {
              case e: IllegalStateException =>
              /*
              I couldn't really find a better way to do this at first glance. There seems to be no boolean method to
              return whether or not a user is logged in (for some reason)
              Will read more docs and if for some reason there truly isn't one, maybe PR it?
               */
            }
          }
          logger.info("Entering execution loop")
          val vultureClient = new VultureClient
          httpServer.vultureClientOption = Some(vultureClient)
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
