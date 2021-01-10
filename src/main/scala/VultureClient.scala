package pw.byakuren.redditmonitor

import watcher.VultureWatcher

import jsonmodels.vultureconfig.VultureConfig
import net.dean.jraw.RedditClient
import net.dean.jraw.models.{Submission, SubredditSort}
import net.dean.jraw.references.SubredditReference

import java.util.concurrent.{BlockingQueue, Executors, LinkedBlockingQueue, TimeUnit}
import java.util.logging.Logger
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._

class VultureClient(client: RedditClient)(implicit config: VultureConfig) extends Runnable {

  private val internalThreadPool = Executors.newScheduledThreadPool(config.maxThreads.toInt)

  private val vultureWatchers = VultureWatcher.loadAllFromConfiguration(client)

  private val monitoredSubredditMap: Map[SubredditReference, Seq[VultureWatcher]] = vultureWatchers.groupBy(_.subreddit)
  private def monitoredSubreddits: Seq[SubredditReference] = monitoredSubredditMap.keys.toSeq

  private val seenPosts: Map[SubredditReference, mutable.HashSet[String]] =
    monitoredSubreddits.map((_, new mutable.HashSet[String]())).toMap

  private val newPosts: Map[SubredditReference, BlockingQueue[Submission]] =
    monitoredSubreddits.map(sr => (sr, new LinkedBlockingQueue[Submission])).toMap

  //This is here to ensure a post won't be acted on several times
  private val actedOnIds: mutable.Seq[String] = new ArrayBuffer[String]()

  private val subredditIntervals: Map[SubredditReference, Int] = vultureWatchers.groupMap(_.subreddit)(vw => vw.interval)
    .map(t => (t._1, t._2.sortWith(_>_).head))

  private val logger = Logger.getLogger("VultureClient")

  override def run(): Unit = {
    monitoredSubreddits.foreach(subreddit => {
      val interval = subredditIntervals(subreddit)
      logger.info(s"r/${subreddit.getSubreddit} will fetch new posts every $interval seconds")
      internalThreadPool.scheduleAtFixedRate(() => {
        val posts = findNewPosts(subreddit)
        newPosts(subreddit).addAll(posts.asJavaCollection)
        seenPosts(subreddit).addAll(posts.map(_.getUniqueId))
      },
        0, interval, TimeUnit.SECONDS)
    })

  }

  def findNewPosts(subreddit: SubredditReference): Set[Submission] = {
    logger.info(s"Fetching new posts for r/${subreddit.getSubreddit}")
    val posts = subreddit.posts().sorting(SubredditSort.NEW).limit(20 /* Make this a config option?*/).build()
    val currentNewPosts = posts.accumulateMerged(1).asScala.filter(post => !seenPosts(subreddit).contains(post.getUniqueId))
    logger.info(s"r/${subreddit.getSubreddit} has ${currentNewPosts.size} new posts")
    currentNewPosts.toSet
  }
}
