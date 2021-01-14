package pw.byakuren.redditmonitor.auth

import java.net.InetSocketAddress
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import net.dean.jraw.RedditClient
import net.dean.jraw.oauth.StatefulAuthHelper
import pw.byakuren.redditmonitor.AuthMode.AuthMode
import pw.byakuren.redditmonitor.{AuthMode, VultureClient}

import java.util.logging.{LogManager, Logger}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise, blocking}
import scala.util.Try

class VultureHTTP(stopCallback: () => Unit)(implicit authMode: AuthMode) {

  var vultureClientOption: Option[VultureClient] = None

  private val logger: Logger = Logger.getLogger("RedditOAuth")
  logger.info("Spawning HTTP server")
  private val internalServer: HttpServer = HttpServer.create(new InetSocketAddress("localhost", 58497), 0)

  internalServer.createContext("/config", { httpE: HttpExchange =>
    serveConfigPage(httpE)
  }: HttpHandler)

  internalServer.createContext("/stop", { httpE: HttpExchange =>
    stopCallback()
  }: HttpHandler)

  internalServer.createContext("/status", { httpE: HttpExchange =>
    serveStatusPage(httpE)
  }: HttpHandler)

  internalServer.setExecutor(null)
  internalServer.start()


  def authenticate(helper: StatefulAuthHelper): Future[Option[RedditClient]] = {

    val rcs = Promise[String]

    internalServer.createContext("/redditAuth", { httpExchange =>
      logger.info("Received HTTP exchange")
      rcs.complete(Try {
        val data = httpExchange.getRequestURI.getQuery.split("&").map(i => {
          val r = i.indexOf("=")
          val f = i.substring(0, r)
          val s = i.substring(r + 1, i.length)
          (f, s)
        }).toMap
        if (data.contains("error")) {
          logger.warning("Authentication failure")
          val error = data("error")
          serveFailurePage(httpExchange, error)
          throw new Exception(s"data contained 'error': $error")
        } else {
          logger.info("Received response code from reddit")
          serveSuccessPage(httpExchange)
          "http://localhost:58497" + httpExchange.getRequestURI.toString
        }
      })
    }: HttpHandler)

    rcs.future.map { str =>
      blocking {
        internalServer.removeContext("/redditAuth")
        Option(helper.onUserChallenge(str))
      }
    }
  }

  private def serveSuccessPage(httpExchange: HttpExchange): Unit = {
    val pageRes = getClass.getClassLoader.getResourceAsStream("html/good.html")
    val content = pageRes.readAllBytes()
    replyOK(httpExchange, content)
  }

  private def serveFailurePage(httpExchange: HttpExchange, error: String): Unit = {
    val pageRes = getClass.getClassLoader.getResourceAsStream("html/bad.html")
    val content = new String(pageRes.readAllBytes()).replace("$error", error).getBytes
    replyOK(httpExchange, content)
  }

  private def serveConfigPage(httpExchange: HttpExchange): Unit = {
    val pageRes = getClass.getClassLoader.getResourceAsStream("html/config_generator.html")
    val content = pageRes.readAllBytes()
    replyOK(httpExchange, content)
  }

  private def serveStatusPage(httpExchange: HttpExchange): Unit = {
    val pageRes = getClass.getClassLoader.getResourceAsStream("html/status.html")
    val stringContent = new String(pageRes.readAllBytes())
    val vultureClient = vultureClientOption.get

    val replaceMap: Map[String, String] = Map(
      "$authMode" -> {
        authMode match {
          case AuthMode.User => "user"
          case AuthMode.Userless => "userless"
          case _ => "unknown"
        }
      },
      "$user" -> {
        try {
          vultureClient.client.requireAuthenticatedUser()
        } catch {
          case e: Exception => "n/a"
        }
      },
      "$subCount" -> vultureClient.monitoredSubreddits.size.toString,
      "$watcherCount" -> vultureClient.vultureWatchers.size.toString,
      "$watcherInfo" -> vultureClient.vultureWatchers.mkString("\n"),
      "$postsScanned" -> vultureClient.seenPosts.map(_._2.size).sum.toString
    )

    val imp = replaceMap.foldLeft(stringContent) { (s, t) =>
      s.replace(t._1, t._2)
    }

    replyOK(httpExchange, imp.getBytes)
  }

  private def replyOK(httpExchange: HttpExchange, content: Array[Byte]): Unit = {
    httpExchange.sendResponseHeaders(200, content.size)
    val os = httpExchange.getResponseBody
    os.write(content)
    os.close()
  }

}
