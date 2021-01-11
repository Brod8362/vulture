package pw.byakuren.redditmonitor
package watcher.action

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.send.{WebhookEmbed, WebhookEmbedBuilder}
import net.dean.jraw.RedditClient
import net.dean.jraw.models.Submission

class WebhookAction(url: String) extends SubmissionAction {

  private val webhookClient = WebhookClient.withUrl(url)

  override def name: String = "webhook"

  override def arguments: Seq[Any] = Seq(url)

  override def run(post: Submission)(implicit client: RedditClient): Unit = {
    val embed = new WebhookEmbedBuilder()
      .setTitle(new WebhookEmbed.EmbedTitle(s"${post.getTitle} (r/${post.getSubreddit})", "http://reddit.com/"+post.getPermalink))
      .setDescription(Option(post.getSelfText).getOrElse("") take 100)
      .setColor(0xFFFF00)

    if (Option(post.getThumbnail).getOrElse("") != "") {
      embed.setThumbnailUrl(post.getThumbnail)
    }

    webhookClient.send(embed.build())
  }

  override def create(args: Seq[Any]): SubmissionAction = new WebhookAction(args.head.toString)
}
