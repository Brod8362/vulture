package pw.byakuren.redditmonitor
package watcher.action

trait Action[T] {

  def name: String
  def arguments: Seq[Any]
  def run(obj: T): Unit

}
