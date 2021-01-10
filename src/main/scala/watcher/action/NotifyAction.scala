package pw.byakuren.redditmonitor
package watcher.action

import net.dean.jraw.RedditClient
import net.dean.jraw.models.Submission

class NotifyAction(content: String) extends SubmissionAction {
  override def name: String = "notify"

  override def arguments: Seq[Any] = Seq(content)

  override def run(post: Submission)(implicit client: RedditClient): Unit = {
    client.me().inbox().compose(client.me().getUsername, s"new post in r/${post.getSubreddit}", content+s"\nhttps://reddit.com${post.getPermalink}")
  }

  override def create(args: Seq[Any]): SubmissionAction = new NotifyAction(args.head.toString)
}
