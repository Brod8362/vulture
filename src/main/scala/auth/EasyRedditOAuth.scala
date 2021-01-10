package pw.byakuren.redditmonitor
package auth

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
            val response = s"There was an error authenticating: $error"
            httpExchange.sendResponseHeaders(200, response.length)
            val os = httpExchange.getResponseBody
            os.write(response.getBytes())
            os.close()
            ready=true
          } else {
            val response = s"Authenticated successfully"
            httpExchange.sendResponseHeaders(200, response.length)
            val os = httpExchange.getResponseBody
            os.write(response.getBytes())
            os.close()
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

}
