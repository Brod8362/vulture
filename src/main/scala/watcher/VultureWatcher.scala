package pw.byakuren.redditmonitor
package watcher

import jsonmodels.vultureconfig.{Actions, VultureConfig, Watchers}
import net.dean.jraw.RedditClient
import net.dean.jraw.models.Submission
import net.dean.jraw.references.SubredditReference
import pw.byakuren.redditmonitor.AuthMode.AuthMode
import pw.byakuren.redditmonitor.watcher.action.{ActionsParser, SubmissionAction}

import java.util.logging.Logger

//todo add actions once they're implemented
class VultureWatcher(val id: Int, val name: String, val subreddit: SubredditReference, val interval: Int, matchEither: Boolean,
                     val maxPosts: Int, titleRegex: String, contentRegex: String, actions: Seq[SubmissionAction]) {

  private val logger = Logger.getLogger(s"VultureWatcher-$id(r/${subreddit.getSubreddit})")

  /**
   * Check if the client should act on a post, and act on it if it should.
   *
   * @param post Post to check and potentially act on
   * @return whether or not the client acted
   */
  def checkThenAct(post: Submission)(implicit client: RedditClient): Boolean = {
    val b = willAct(post)
    if (b) {
      act(post)
    }
    b
  }

  /**
   * Determines whether or not the bot should 'act' on a post, using the predefined actions. This considers the matchEither option.
   *
   * @param post Post to check against
   * @return whether or not the client should act
   */
  def willAct(post: Submission): Boolean = {
    val titleMatches = post.getTitle.matches(titleRegex)
    val contentMatches: Boolean = Option(post.getSelfText) match {
      case Some(content) =>
        content.matches(contentRegex)
      case _ =>
        false
    }
    if (matchEither) {
      titleMatches || contentMatches
    } else {
      titleMatches && contentMatches
    }
  }

  /**
   * Perform the predefined actions on a post
   *
   * @param post Post to act upon
   */
  def act(post: Submission)(implicit client: RedditClient): Unit = {
    actions.foreach(a => {
      try {
        a.run(post)
      } catch {
        case e: Exception =>
          logger.severe(s"Error running '$name'[${a.name}] on ${post.getUniqueId}: ${e.getMessage}")
          e.printStackTrace()
      }
    })
  }

  override def toString = s"VultureWatcher-$id '$name' (/r/${subreddit.getSubreddit}))"
}

object VultureWatcher {

  private var nextId = -1

  def fromConfigWatcher(watcher: Watchers, client: RedditClient)(implicit authMode: AuthMode): VultureWatcher = {
    nextId += 1
    new VultureWatcher(
      nextId,
      watcher.name,
      client.subreddit(watcher.subreddit),
      watcher.checkInterval.getOrElse(30.0).toInt,
      watcher.matchEither.getOrElse(false),
      watcher.maxPosts.getOrElse(20.0).toInt,
      watcher.titleRegex.getOrElse(".*"),
      watcher.contentRegex.getOrElse(".*"),
      parseActions(watcher.actions)
    )
  }


  def loadAllFromConfiguration(client: RedditClient)(implicit config: VultureConfig, authMode: AuthMode): Seq[VultureWatcher] = {
    config.watchers.map(fromConfigWatcher(_, client))
  }

  private def parseActions(actions: Seq[Actions])(implicit authMode: AuthMode): Seq[SubmissionAction] = {
    actions.map(ActionsParser.parse).map(_.getOrElse(throw new Exception(s"some action was not found, check config")))
  }

}
