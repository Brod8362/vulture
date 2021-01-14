package pw.byakuren.redditmonitor

import watcher.VultureWatcher

import jsonmodels.vultureconfig.VultureConfig
import net.dean.jraw.RedditClient
import net.dean.jraw.models.{Submission, SubredditSort}
import net.dean.jraw.references.SubredditReference
import pw.byakuren.redditmonitor.AuthMode.AuthMode

import java.util.concurrent.{BlockingQueue, Executors, LinkedBlockingQueue, TimeUnit}
import java.util.logging.{Level, Logger}
import scala.collection.mutable
import scala.jdk.CollectionConverters._

class VultureClient(implicit val client: RedditClient, config: VultureConfig, authMode: AuthMode) extends Runnable {

  private val internalThreadPool = Executors.newScheduledThreadPool(config.maxThreads.toInt)

  val vultureWatchers: Seq[VultureWatcher] = VultureWatcher.loadAllFromConfiguration(client)

  private val monitoredSubredditMap: Map[SubredditReference, Seq[VultureWatcher]] = vultureWatchers.groupBy(_.subreddit)

  def monitoredSubreddits: Seq[SubredditReference] = monitoredSubredditMap.keys.toSeq

  //todo memory concern: this will grow in size unbounded until OOM.
  val seenPosts: Map[SubredditReference, mutable.HashSet[String]] =
    monitoredSubreddits.map((_, new mutable.HashSet[String]())).toMap

  private val newPosts: Map[SubredditReference, BlockingQueue[Submission]] =
    monitoredSubreddits.map(sr => (sr, new LinkedBlockingQueue[Submission])).toMap

  //This is here to ensure a post won't be acted on several times
  private var actedOnIds: mutable.HashSet[String] = new mutable.HashSet[String]()

  private val subredditIntervals: Map[SubredditReference, Int] = vultureWatchers.groupMap(_.subreddit)(vw => vw.interval)
    .map(t => (t._1, t._2.sortWith(_ > _).head)) //get smallest of all watchers

  private val subredditMaxFetch: Map[SubredditReference, Int] = vultureWatchers.groupMap(_.subreddit)(vw => vw.maxPosts)
    .map(t => (t._1, t._2.sortWith(_ < _).head)) //get largest of all watchers

  private val logger = Logger.getLogger("VultureClient")

  override def run(): Unit = {
    monitoredSubreddits.foreach(subreddit => {
      /*
      A subreddit's interval is determined by the LOWEST interval of all watchers configured to that subreddit.
      This is done to avoid querying for the same posts multiple times, hopefully helping save API calls and
      steering away from the rate limit.
       */
      val interval = subredditIntervals(subreddit)

      /*
      A subreddit's maximum fetch is determined in a similar way to the interval, except it uses the LARGEST
      fetch of all of the watchers configured to a given subreddit. This is simply so a watcher expecting X posts
      does not get < X posts. This wouldn't break anything, but it would also not be the intended behavior.
       */
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

    /*
    The previous implementation of this did not support multiple watchers of the same subreddit,
    even though the rest of the foundation is there. There was a race condition as multiple
    threads would consume from the same queue, meaning threads would be starved or just miss posts.
    Now it should do that properly.

    This maybe still be able to be improved by dispatching each watcher with it's own thread when a post is consumed,
    but it's far more likely that would cause more issues than the potential performance gains that could be had.
    There would be an excessively large number of threads for even a sensible/moderate configuration.
    Doing this sequentially may be slower and introduce bottlenecks, but it's better than rapidly hitting the API rate limit.
     */
    monitoredSubredditMap.foreach(t => internalThreadPool.execute { () =>
      val queue = newPosts(t._1)
      while (true) {
        val post = queue.take()
        t._2 foreach {watcher =>
          try {
            if (!actedOnIds.contains(post.getUniqueId) && watcher.checkThenAct(post)) {
              logger.fine(s"${watcher.name}-${watcher.id} handling post ${post.getUniqueId} from r/${post.getSubreddit}")
            }
          } catch {
            case e: Exception =>
              logger.log(Level.SEVERE, e.getMessage, e)
          }
        }
        /*
        Add the post to the "used" ID list only after all of the watchers have had their share.
         */
        actedOnIds += post.getUniqueId
      }
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
