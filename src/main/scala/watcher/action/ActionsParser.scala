package pw.byakuren.redditmonitor
package watcher.action

import jsonmodels.vultureconfig.Actions

case object ActionsParser {

  private val actions = Seq(new CommentAction(""), new DownvoteAction, new MessageAction("", ""),
    new SaveAction(), new UpvoteAction, new DownloadAction(""), new NothingAction, new NotifyAction(""))

  val actionMap: Map[String, SubmissionAction] = actions.map(a => (a.name, a)).toMap

  def parse(jsonAction: Actions): Option[SubmissionAction] = {
    actionMap(jsonAction.`type`) match {
      case a: MessageAction => Some(new MessageAction(jsonAction.arguments.content.getOrElse(throw new Exception("missing title argument"))
        , jsonAction.arguments.content.getOrElse(throw new Exception("missing content argument"))))
      case b: CommentAction => Some(new CommentAction(jsonAction.arguments.content.getOrElse("missing content argument")))
      case d: DownloadAction =>
        if (jsonAction.arguments.fileFormat.isDefined) {
          Some(new DownloadAction(jsonAction.arguments.downloadPath.getOrElse(throw new Exception("missing path argument")), jsonAction.arguments.fileFormat.get))
        } else {
          Some(new DownloadAction(jsonAction.arguments.downloadPath.getOrElse(throw new Exception("missing path argument"))))
        }
      case e: NotifyAction => Some(new NotifyAction(jsonAction.arguments.content.getOrElse("no additional info configured")))
      //general catch all
      case c: SubmissionAction => Some(c.create(Seq()))
      case _ => None
    }
  }

}
