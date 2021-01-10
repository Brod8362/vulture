package pw.byakuren.redditmonitor
package watcher.action
import net.dean.jraw.RedditClient
import net.dean.jraw.models.Submission

class CommentAction(content: String) extends SubmissionAction {
  override def name: String = "comment"

  override def arguments: Seq[Any] = Seq(content)

  override def run(post: Submission)(implicit client: RedditClient): Unit = {
    client.submission(post.getUniqueId).reply(content)
  }

  override def create(args: Seq[Any]): SubmissionAction = {
    new CommentAction(args.head.toString)
  }
}
