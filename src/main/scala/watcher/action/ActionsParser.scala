package pw.byakuren.redditmonitor
package watcher.action

import jsonmodels.vultureconfig.Actions
import pw.byakuren.redditmonitor.AuthMode.AuthMode

case object ActionsParser {

  private val actions = Seq(new CommentAction(""), new DownvoteAction, new MessageAction("", ""),
    new SaveAction(), new UpvoteAction, new DownloadAction(""), new NothingAction, new NotifyAction(""),
    new WebhookAction("https://canary.discord.com/api/webhooks/12345/67890abcdef"))

  val actionMap: Map[String, SubmissionAction] = actions.map(a => (a.name, a)).toMap

  def parse(jsonAction: Actions)(implicit authMode: AuthMode): Option[SubmissionAction] = {
    val submissionActionOption: Option[SubmissionAction] = actionMap(jsonAction.`type`) match {
      case a: MessageAction => Some(new MessageAction(jsonAction.arguments.content.getOrElse(throw new Exception("missing title argument"))
        , jsonAction.arguments.content.getOrElse(throw new Exception("missing content argument"))))
      case b: CommentAction => Some(new CommentAction(jsonAction.arguments.content.getOrElse("missing content argument")))
      case d: DownloadAction =>
        if (jsonAction.arguments.fileFormat.isDefined) {
          Some(new DownloadAction(jsonAction.arguments.downloadPath.getOrElse(throw new Exception("missing path argument")), jsonAction.arguments.fileFormat.get))
        } else {
          Some(new DownloadAction(jsonAction.arguments.downloadPath.getOrElse(throw new Exception("missing path argument"))))
        }
      case e: NotifyAction => Some(new NotifyAction(jsonAction.arguments.content.getOrElse("no additional info configured"), jsonAction.arguments.destUser))
      case f: WebhookAction =>
        Some(new WebhookAction(jsonAction.arguments.webhookUrl.getOrElse("no webhook URL provided"), jsonAction.arguments.content))
      //general catch all
      case c: SubmissionAction => Some(c.create(Seq()))
      case _ => None
    }
    if (submissionActionOption.isDefined && submissionActionOption.get.minimumAuthLevel < authMode) {
      throw new RuntimeException(s"Auth level for action ${submissionActionOption.get.name} not met")
    }
    submissionActionOption
  }

}
