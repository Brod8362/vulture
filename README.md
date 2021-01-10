Vulture
=======
A configurable program to monitor and act on new reddit posts.

Setup
=====

Download the latest jar from the [releases page](https://github.com/Brod8362/vulture/releases).

Put the jar file in a folder somewhere. If you're on Windows, you should be able to just double-click it to run it. If
not, create the following file and call it `start.bat`:

**start.bat**

```batch
@echo off
java -jar JARFILE
```

Make sure to replace `JARFILE` with the name of the jar you downloaded.

If you're on Linux or macOS, instead create the following

**start.sh**

```shell
#!/bin/sh
java -jar JARFILE
```

Again, replace JARFILE with the name of the .jar you downloaded. You may need to `chmod +x start.sh` if it won't allow
you to execute it.

On the first run, `vultureConfig.json` will be automatically created with the default settings. These defaults are not
very sensible or useful, so it's important to understand how to configure them to your liking. See below for
configuration.

Options
=======
__Root Options__

```json5
//Control how many threads are allowed to exist at once.
//Best to leave this alone, unless you are runing a lot of monitors at once.
"maxThreads": 32,
        
//Watchers to use. See below.
"watchers": []
```

__Watcher Options__

```json5
{
  "name": "name of your watcher", //Required.
  "subreddit": "linux", //The name of your subreddit, WITHOUT the r/ in front. Required.

  //The regex you want to match post titles against. .* all content will match. 
  //This is optional and will default to .* 
  "titleRegex": ".*",
  //The regex you want to match post content against. .* all content will match. 
  //This is optional and will default to .*
  "contentRegex": ".*",
  //See below for a link about how to use regex.

  //If this is false, both titleRegex and contentRegex must match to act on your post.
  //If this is true, either one can match.
  //This is optional and defaults to false.
  "matchEither": false,
  //How frequently you want to check the subreddit for new posts (in seconds)
  //This is optional and defaults to 30 seconds.
  "checkInterval": 30,
  //This is how many posts will be pulled at once.
  //If the sub is very active and is skipping posts, increase this number.
  //This is optional and defaults to 20.
  "maxPosts": 20,
  //An array of actions. See below.
  //Required.
  "actions": []
}
```

__Action Options__

```json5
{
  //Action type. See table below for a list of all actions and their arguments
  "type": "nothing",
  //An array of arguments. Required, but may be empty.
  "arguments": {}
}
```

Learn more about regex [here](https://medium.com/factory-mind/regex-tutorial-a-simple-cheatsheet-by-examples-649dc1c3f285).

All arguments are required unless otherwise specified.

| **Type**   | **Argument(s)**                             | **Behavior**                                                                                              | **Notes**                                 |
|------------|---------------------------------------------|-----------------------------------------------------------------------------------------------------------|-------------------------------------------|
| *nothing*  | -                                           | does nothing                                                                                              | intended for debugging                    |
| *comment*  | `content`                                   | leaves a comment with content `content`.                                                                  |                                           |
| *downvote* | -                                           | downvotes the post                                                                                        | *Please* don't make downvote bots.        |
| *upvote*   | -                                           | upvotes the post                                                                                          | *Please* don't make upvote bots.          |
| *message*  | `title`,`content`                           | sends a PM the author of the post with title `title` and content `content`                                |                                           |
| *download* | `downloadPath`, `fileFormat` (optional)     | Download the post to path `downloadPath` with filename following `fileFormat`.                            | Will download images if available.        |
| *notify*   | `content` (optional), `destUser` (optional) | Send a message to `destUser` (default is yourself) with content `content` followed by the post permalink. | Only ever point it at an account you own. |
| *save*     | -                                           | Saves the post to your reddit account.                                                                    | Does not save to local disk               |

As an example, to add the action `message`, you would do
```json
{
  "type": "message",
  "arguments": {
    "title": "I liked your post",
    "content": "Your post was so super cool i really liked it will you please upvote all of my posts?"
  }
}
```

To save a file to disk, you would do
```json
{
  "type": "download",
  "arguments": {
    "fileFormat": "%title% %author%",
    "downloadPath": "redditDownloads"
  }
}
```


__File Format Options (for download)__

| **String**  | **Replaced By**                   |
|-------------|-----------------------------------|
| %id%        | The post's ID                     |
| %title%     | The post's title                  |
| %author%    | The post's author                 |
| %subreddit% | The subreddit name (excluding r/) |
| %flair%     | The post's flair                  |

Pull Requests
=============
Make them, if you'd like

Bugs
====
Report them or fix with a pull request
