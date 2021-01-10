package pw.byakuren.redditmonitor.auth

import java.net.InetSocketAddress

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import net.dean.jraw.RedditClient
import net.dean.jraw.oauth.StatefulAuthHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.Try

object EasyRedditOAuth {
  def authenticate(helper: StatefulAuthHelper): Future[Option[RedditClient]] = {
    val internalServer = HttpServer.create(new InetSocketAddress("localhost", 58497), 0)
    val rcs = Promise[String]

    internalServer.createContext("/redditAuth", new HttpHandler() {
      override def handle(httpExchange: HttpExchange): Unit =
        rcs.complete(Try {
          val data = httpExchange.getRequestURI.getQuery.split("&").map(i => {
            val r = i.indexOf("=")
            val f = i.substring(0, r)
            val s = i.substring(r+1, i.length)
            (f, s)
          }).toMap
          if (data.contains("error")) {
            val error = data("error")
            serveFailurePage(httpExchange, error)
            throw new Exception(s"data contained 'error': $error")
          } else {
            serveSuccessPage(httpExchange)
            "http://localhost:58497" + httpExchange.getRequestURI.toString
          }
        })

    })
    internalServer.setExecutor(null)
    internalServer.start()

    rcs.future.map { str =>
      val opt = Option(helper.onUserChallenge(str))
      internalServer.stop(0)
      opt
    }
  }

  private def serveSuccessPage(httpExchange: HttpExchange): Unit = {
    val pageRes = getClass.getClassLoader.getResourceAsStream("html/good.html")
    val content = pageRes.readAllBytes()
    httpExchange.sendResponseHeaders(200, content.size)
    val os = httpExchange.getResponseBody
    os.write(content)
    os.close()
  }

  private def serveFailurePage(httpExchange: HttpExchange, error: String): Unit = {
    val pageRes = getClass.getClassLoader.getResourceAsStream("html/bad.html")
    val content = new String(pageRes.readAllBytes()).replace("$error", error).getBytes
    httpExchange.sendResponseHeaders(200, content.size)
    val os = httpExchange.getResponseBody
    os.write(content)
    os.close()
  }

}
