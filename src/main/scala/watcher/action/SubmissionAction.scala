package pw.byakuren.redditmonitor
package watcher.action

import net.dean.jraw.RedditClient
import net.dean.jraw.models.Submission

trait SubmissionAction {

  def name: String
  def arguments: Seq[Any]
  def run(post: Submission)(implicit client: RedditClient): Unit
  def create(args: Seq[Any]): SubmissionAction

}
