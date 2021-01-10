package pw.byakuren.redditmonitor
package watcher.action

import net.dean.jraw.RedditClient
import net.dean.jraw.models.Submission

class UpvoteAction extends SubmissionAction {
  override def name: String = "upvote"

  override def arguments: Seq[Any] = Seq()

  override def run(obj: Submission)(implicit client: RedditClient): Unit = {
    client.submission(obj.getUniqueId).upvote()
  }

  override def create(args: Seq[Any]): SubmissionAction = new UpvoteAction
}
