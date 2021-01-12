package pw.byakuren.redditmonitor
package watcher.action

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.send.{WebhookEmbed, WebhookEmbedBuilder, WebhookMessage, WebhookMessageBuilder}
import net.dean.jraw.RedditClient
import net.dean.jraw.models.Submission
import pw.byakuren.redditmonitor.AuthMode.AuthMode

class WebhookAction(url: String, extraContent: Option[String] = None) extends SubmissionAction {

  private val webhookClient = WebhookClient.withUrl(url)

  override def name: String = "webhook"

  override def arguments: Seq[Any] = Seq(url)

  override def run(post: Submission)(implicit client: RedditClient): Unit = {
    val embed = new WebhookEmbedBuilder()
      .setTitle(new WebhookEmbed.EmbedTitle(s"${post.getTitle} (r/${post.getSubreddit})", "http://reddit.com/"+post.getPermalink))
      .setDescription(Option(post.getSelfText).getOrElse("") take 100)
      .setColor(0xFFFF00)

    Option(post.getThumbnail) match {
      case Some(url) if url != "" && url != "self" =>
        embed.setThumbnailUrl(url)
      case _ =>
    }

    val msgBuilder = new WebhookMessageBuilder()
      .addEmbeds(embed.build())
      .setContent(extraContent.orNull)

    try {
      webhookClient.send(msgBuilder.build())
    } catch {
      case e:Exception =>
        e.printStackTrace()
    }
  }

  override def create(args: Seq[Any]): SubmissionAction = new WebhookAction(args.head.toString)

  override def minimumAuthLevel: AuthMode = AuthMode.Userless
}
