package pw.byakuren.redditmonitor
package watcher.action

import net.dean.jraw.RedditClient
import net.dean.jraw.models.Submission
import pw.byakuren.redditmonitor.AuthMode.AuthMode

class MessageAction(title: String, content: String) extends SubmissionAction {
  override def name: String = "message"

  override def arguments: Seq[Any] = Seq(content)

  override def run(post: Submission)(implicit client: RedditClient): Unit = {
    client.me().inbox().compose(post.getAuthor, title, content)
  }

  override def create(args: Seq[Any]): SubmissionAction = new MessageAction(args.head.toString, args(1).toString)

  override def minimumAuthLevel: AuthMode = AuthMode.User
}
