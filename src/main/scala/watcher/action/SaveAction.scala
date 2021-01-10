package pw.byakuren.redditmonitor
package watcher.action

import net.dean.jraw.RedditClient
import net.dean.jraw.models.Submission

class SaveAction extends SubmissionAction {
  override def name: String = "save"

  override def arguments: Seq[Any] = Seq()

  override def run(obj: Submission)(implicit client: RedditClient): Unit = {
    client.submission(obj.getUniqueId).save()
  }

  override def create(args: Seq[Any]): SubmissionAction = new SaveAction
}