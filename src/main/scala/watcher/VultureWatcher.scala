package pw.byakuren.redditmonitor
package watcher

import jsonmodels.vultureconfig.{VultureConfig, Watchers}
import net.dean.jraw.RedditClient
import net.dean.jraw.models.Submission
import net.dean.jraw.references.SubredditReference
import pw.byakuren.redditmonitor.watcher.action.Action

//todo add actions once they're implemented
class VultureWatcher(val name: String, val subreddit: SubredditReference, titleRegex: Option[String],
                     contentRegex: Option[String], actions: Seq[Action[Submission]]) {

  def checkThenAct(post: Submission): Boolean = {
    val willAct = willAct(post)
    if (willAct) {
      willAct(post)
    }
    willAct
  }

  def willAct(post: Submission): Boolean = {
    //todo implement
    false
  }

  def act(post: Submission): Unit = {
    //todo implement
  }


}

object VultureWatcher {

  def fromConfigWatcher(watcher: Watchers, client: RedditClient): VultureWatcher = {
    new VultureWatcher(
      watcher.name,
      client.subreddit(watcher.subreddit),
      Option(watcher.titleRegex),
      Option(watcher.contentRegex),
      Seq() //todo parse actions
    )
  }


  def loadAllFromConfiguration(client: RedditClient)(implicit config: VultureConfig): Seq[VultureWatcher] = {
    config.watchers.map(fromConfigWatcher(_, client))
  }

}
