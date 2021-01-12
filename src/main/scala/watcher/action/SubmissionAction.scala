package pw.byakuren.redditmonitor
package watcher.action

import net.dean.jraw.RedditClient
import net.dean.jraw.models.Submission
import pw.byakuren.redditmonitor.AuthMode.AuthMode

trait SubmissionAction {

  def name: String
  def arguments: Seq[Any]
  def run(post: Submission)(implicit client: RedditClient): Unit
  def create(args: Seq[Any]): SubmissionAction
  def minimumAuthLevel: AuthMode

}
