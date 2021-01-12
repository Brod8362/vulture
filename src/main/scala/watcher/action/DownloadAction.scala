package pw.byakuren.redditmonitor
package watcher.action

import net.dean.jraw.RedditClient
import net.dean.jraw.models.Submission
import play.api.libs.json.Json
import pw.byakuren.redditmonitor.AuthMode.AuthMode

import java.io.{File, FileOutputStream}
import java.net.URL
import java.nio.file.{Files, Paths, StandardCopyOption}

class DownloadAction(pathStr: String, namingFormat: String = "%id%") extends SubmissionAction {

  override def name: String = "download"

  override def arguments: Seq[Any] = Seq(pathStr)

  override def run(post: Submission)(implicit client: RedditClient): Unit = {
    val postId = post.getUniqueId
    val path = new File(pathStr)
    path.mkdirs()
    val hasContent = post.getEmbeddedMedia != null && post.getEmbeddedMedia.getOEmbed!=null
    val hasRedditImage = post.getSelfText == null || post.getSelfText == ""
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
      "otherContentUrl" -> {if (hasContent) post.getEmbeddedMedia.getOEmbed.getUrl else null},
      "hasRedditImage" -> hasRedditImage
    )
    val filename = formatFilename(post)
    val jfos = new FileOutputStream(new File(path.getAbsolutePath+s"/$filename"))
    jfos.write(json.toString().getBytes())
    jfos.close()

    if (hasContent) {
      saveFile(post.getEmbeddedMedia.getOEmbed.getUrl)
    }
    if (hasRedditImage) {
      saveFile(post.getUrl)
    }
  }

  override def create(args: Seq[Any]): SubmissionAction = new DownloadAction(args.head.toString)

  private def saveFile(url: String): Unit = {
    val is = new URL(url).openStream()
    Files.copy(is, Paths.get(pathStr+"/"+url.split("/").last), StandardCopyOption.REPLACE_EXISTING)
  }

  /**
   * Format the filename for the post json file. Available formats:
   * %id% -> id of post
   * %author% -> username of post author
   * %title% -> title of post
   * %subreddit% -> post subreddit
   * %flair% -> post flair
   * @param post post to get format info from
   * @return formatted filename
   */
  private def formatFilename(post: Submission): String = {
    val replacements = Map(
      "%id%" -> Option(post.getUniqueId),
      "%author%" -> Option(post.getAuthor),
      "%title%" -> Option(post.getTitle),
      "%subreddit%" -> Option(post.getSubreddit),
      "%flair%" -> Option(post.getLinkFlairText),
    )
    var str = namingFormat+""
    replacements.foreach(t => str = str.replace(t._1, t._2.getOrElse("null")))
    str+".json"
  }

  override def minimumAuthLevel: AuthMode = AuthMode.Userless
}
