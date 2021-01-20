package pw.byakuren.redditmonitor

import scala.util.control.NonFatal

object MetaInfo {

  //definitely not stolen from https://github.com/ScoreUnder/canti-bot/blob/master/src/main/scala/score/discord/generalbot/BotMeta.scala
  //hey, that's the point of open source ain't it?

  private def getPackageInfo(f: Package => String): Option[String] =
    try Option(f(getClass.getPackage)) catch {
      case NonFatal(_) => None
    }

  lazy val VERSION: String = getPackageInfo(_.getImplementationVersion).getOrElse("unknown")

}
