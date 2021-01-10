package pw.byakuren.redditmonitor

import watcher.VultureWatcher

import jsonmodels.vultureconfig.VultureConfig
import net.dean.jraw.RedditClient
import net.dean.jraw.models.Subreddit
import net.dean.jraw.references.SubredditReference

import java.util.concurrent.Executors

class VultureClient(client: RedditClient)(implicit config: VultureConfig) extends Runnable {

  private val internalThreadPool = Executors.newScheduledThreadPool(config.maxThreads.toInt)

  private val vultureWatchers = VultureWatcher.loadAllFromConfiguration(client)

  private val monitoredSubreddits: Seq[SubredditReference] = vultureWatchers.map(_.subreddit)
  private val monitoredSubredditMap: Map[SubredditReference, Seq[VultureWatcher]] = vultureWatchers.groupBy(_.subreddit)

  override def run(): Unit = {

  }

  def dispatchSubredditCheckers(): Unit = {

  }
}
