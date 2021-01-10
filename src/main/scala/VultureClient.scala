package pw.byakuren.redditmonitor

import watcher.VultureWatcher

import jsonmodels.vultureconfig.VultureConfig
import net.dean.jraw.RedditClient
import net.dean.jraw.models.{Submission, Subreddit, SubredditSort}
import net.dean.jraw.references.SubredditReference

import java.util.concurrent.{BlockingQueue, Executors, LinkedBlockingQueue}
import java.util.logging.Logger
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class VultureClient(client: RedditClient)(implicit config: VultureConfig) extends Runnable {

  private val internalThreadPool = Executors.newScheduledThreadPool(config.maxThreads.toInt)

  private val vultureWatchers = VultureWatcher.loadAllFromConfiguration(client)

  private val monitoredSubreddits: Seq[SubredditReference] = vultureWatchers.map(_.subreddit)
  private val monitoredSubredditMap: Map[SubredditReference, Seq[VultureWatcher]] = vultureWatchers.groupBy(_.subreddit)

  private val newPosts: Map[SubredditReference, BlockingQueue[Submission]] =
    monitoredSubreddits.map(sr => (sr, new LinkedBlockingQueue[Submission])).toMap

  //This is here to ensure a post won't be acted on several times
  private val actedOnIds: mutable.Seq[String] = new ArrayBuffer[String]()

  private val logger = Logger.getLogger("VultureClient")

  override def run(): Unit = {

  }

  def dispatchSubredditCheckers(): Unit = {
    monitoredSubreddits.foreach({subreddit =>
      subreddit.posts().sorting(SubredditSort.NEW)
    })
  }
}
