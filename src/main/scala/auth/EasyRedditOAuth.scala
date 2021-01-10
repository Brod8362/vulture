package pw.byakuren.redditmonitor.auth

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import net.dean.jraw.RedditClient
import net.dean.jraw.oauth.StatefulAuthHelper

import java.net.InetSocketAddress
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object EasyRedditOAuth {

  def authenticate(helper: StatefulAuthHelper): Future[Option[RedditClient]] = {
    Future {
      val internalServer = HttpServer.create(new InetSocketAddress("localhost", 58497), 0)
      var rcs: Option[String] = None
      var ready = false

      internalServer.createContext("/redditAuth", new HttpHandler() {
        override def handle(httpExchange: HttpExchange): Unit = {
          val data = httpExchange.getRequestURI.getQuery.split("&").map(i => {
            val r = i.indexOf("=")
            val f = i.substring(0, r)
            val s = i.substring(r+1, i.length)
            (f, s)
          }).toMap
          if (data.contains("error")) {
            val error = data("error")
            try {
              serveFailurePage(httpExchange, error)
            } catch {
              case e: Exception => println("failed to serve failure page")
            }
            ready=true
          } else {
            try {
              serveSuccessPage(httpExchange)
            } catch {
              case e: Exception => println("failed to serve success page")
            }
            rcs = Some("http://localhost:58497"+httpExchange.getRequestURI.toString)
            ready=true
          }
        }

      })
      internalServer.setExecutor(null)
      internalServer.start()

      while (!ready) {
        Thread.sleep(1000)
        //TODO THIS IS ABHORRENT BUT FUTURES ARE HARD
      }
      rcs match {
        case Some(str) =>
          println(str)
          val opt = Option(helper.onUserChallenge(str))
          internalServer.stop(0)
          opt
        case None =>
          None
      }
    }
  }

  private def serveSuccessPage(httpExchange: HttpExchange): Unit = {
    val pageRes = getClass.getResourceAsStream("html/good.html")
    val content = pageRes.readAllBytes()
    httpExchange.sendResponseHeaders(200, content.size)
    val os = httpExchange.getResponseBody
    os.write(content)
    os.close()
  }

  private def serveFailurePage(httpExchange: HttpExchange, error: String): Unit = {
    val pageRes = getClass.getResourceAsStream("html/bad.html")
    val content = new String(pageRes.readAllBytes()).replace("$error", error).getBytes
    httpExchange.sendResponseHeaders(200, content.size)
    val os = httpExchange.getResponseBody
    os.write(content)
    os.close()
  }

}
