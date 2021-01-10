package pw.byakuren.redditmonitor
package watcher.action

import net.dean.jraw.RedditClient
import net.dean.jraw.models.Submission
import play.api.libs.json.Json

import java.io.{File, FileOutputStream}
import java.net.URL
import java.nio.file.{Files, Paths, StandardCopyOption}

class DownloadAction(pathStr: String) extends SubmissionAction {
  override def name: String = "download"

  override def arguments: Seq[Any] = Seq(pathStr)

  override def run(post: Submission)(implicit client: RedditClient): Unit = {
    val postId = post.getUniqueId
    val path = new File(pathStr)
    path.mkdirs()
    val hasContent = post.getEmbeddedMedia!= null && post.getEmbeddedMedia.getOEmbed!=null
    val json = Json.obj(
      "postId" -> postId,
      "permalink" -> post.getPermalink,
      "url" -> post.getUrl,
      "title" -> post.getTitle,
      "author" -> post.getAuthor,
      "authorFlair" -> post.getAuthorFlairText,
      "postFlair" -> post.getLinkFlairText,
      "text" -> post.getSelfText,
      "created" -> post.getCreated.toString,
      "nsfw" -> post.isNsfw,
      "thumbnailUrl" -> post.getThumbnail,
      "hasOtherContent" -> hasContent,
      "otherContentUrl" -> {if (hasContent) post.getEmbeddedMedia.getOEmbed.getUrl else null}
    )
    val jfos = new FileOutputStream(new File(path.getAbsolutePath+s"/$postId.json"))
    jfos.write(json.toString().getBytes())
    jfos.close()

    if (hasContent) {
      val is = new URL(post.getEmbeddedMedia.getOEmbed.getUrl).openStream()
      Files.copy(is, Paths.get(post.getEmbeddedMedia.getOEmbed.getUrl.split("/").last), StandardCopyOption.REPLACE_EXISTING)
    }
  }

  override def create(args: Seq[Any]): SubmissionAction = new DownloadAction(args.head.toString)
}
