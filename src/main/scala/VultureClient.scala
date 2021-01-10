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

class VultureClient(implicit client: RedditClient, config: VultureConfig) extends Runnable {

  private val internalThreadPool = Executors.newScheduledThreadPool(config.maxThreads.toInt)

  private val vultureWatchers = VultureWatcher.loadAllFromConfiguration(client)

  private val monitoredSubredditMap: Map[SubredditReference, Seq[VultureWatcher]] = vultureWatchers.groupBy(_.subreddit)

  private def monitoredSubreddits: Seq[SubredditReference] = monitoredSubredditMap.keys.toSeq

  //todo memory concern: this will grow in size unbounded until OOM.
  private val seenPosts: Map[SubredditReference, mutable.HashSet[String]] =
    monitoredSubreddits.map((_, new mutable.HashSet[String]())).toMap

  private val newPosts: Map[SubredditReference, BlockingQueue[Submission]] =
    monitoredSubreddits.map(sr => (sr, new LinkedBlockingQueue[Submission])).toMap

  //This is here to ensure a post won't be acted on several times
  private var actedOnIds: ArrayBuffer[String] = new ArrayBuffer[String]()

  private val subredditIntervals: Map[SubredditReference, Int] = vultureWatchers.groupMap(_.subreddit)(vw => vw.interval)
    .map(t => (t._1, t._2.sortWith(_ > _).head)) //get smallest of all watchers

  private val subredditMaxFetch: Map[SubredditReference, Int] = vultureWatchers.groupMap(_.subreddit)(vw => vw.maxPosts)
    .map(t => (t._1, t._2.sortWith(_ < _).head)) //get largest of all watchers

  private val logger = Logger.getLogger("VultureClient")

  override def run(): Unit = {
    monitoredSubreddits.foreach(subreddit => {
      val interval = subredditIntervals(subreddit)
      val maxFetch = subredditMaxFetch(subreddit)
      logger.info(s"r/${subreddit.getSubreddit} will fetch up to $maxFetch new posts every $interval seconds")
      internalThreadPool.scheduleAtFixedRate(() => {
        val posts = findNewPosts(subreddit, maxFetch)
        posts.foreach(
          newPosts(subreddit).put
        ) //put all posts into the queue (blocking)
        seenPosts(subreddit).addAll(posts.map(_.getUniqueId))
      },
        0, interval, TimeUnit.SECONDS)
    })
    vultureWatchers.foreach(watcher => {
      internalThreadPool.execute(() => {
        while (true) {
          val post = newPosts(watcher.subreddit).take()
          if (!actedOnIds.contains(post.getUniqueId) && watcher.checkThenAct(post)) {
            logger.fine(s"Handling post ${post.getUniqueId} from r/${post.getSubreddit}")
            actedOnIds += post.getUniqueId
          }
        }
      })
    })
  }

  def findNewPosts(subreddit: SubredditReference, maxFetch: Int): Set[Submission] = {
    logger.info(s"Fetching new posts for r/${subreddit.getSubreddit}")
    val posts = subreddit.posts().sorting(SubredditSort.NEW).limit(maxFetch).build()
    val currentNewPosts = posts.accumulateMerged(1).asScala.filter(post => !seenPosts(subreddit).contains(post.getUniqueId))
    logger.info(s"r/${subreddit.getSubreddit} has ${currentNewPosts.size} new posts")
    currentNewPosts.toSet
  }
}
