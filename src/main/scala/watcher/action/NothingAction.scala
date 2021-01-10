package pw.byakuren.redditmonitor
package watcher.action
import net.dean.jraw.RedditClient
import net.dean.jraw.models.Submission

class NothingAction extends SubmissionAction {
  override def name: String = "nothing"

  override def arguments: Seq[Any] = Seq()

  override def run(post: Submission)(implicit client: RedditClient): Unit = {}

  override def create(args: Seq[Any]): SubmissionAction = new NothingAction
}
