package pw.byakuren.redditmonitor
package watcher

import jsonmodels.vultureconfig.{VultureConfig, Watchers}
import net.dean.jraw.RedditClient
import net.dean.jraw.references.SubredditReference

//todo add actions once they're implemented
class VultureWatcher(val name: String, val subreddit: SubredditReference, titleRegex: Option[String],
                     contentRegex: Option[String]) {


}

object VultureWatcher {

  def fromConfigWatcher(watcher: Watchers, client: RedditClient): VultureWatcher = {
    new VultureWatcher(
      watcher.name,
      client.subreddit(watcher.subreddit),
      Option(watcher.titleRegex),
      Option(watcher.contentRegex),
    )
  }


  def loadAllFromConfiguration(client: RedditClient)(implicit config: VultureConfig): Seq[VultureWatcher] = {
    config.watchers.map(fromConfigWatcher(_, client))
  }

}
