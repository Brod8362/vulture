package pw.byakuren.redditmonitor
package watcher.action

trait Action {

  def name: String
  def arguments: Seq[Any]
  def run(): Unit

}
