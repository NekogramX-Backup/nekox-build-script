package nekox

import cn.hutool.core.util.CharsetUtil
import cn.hutool.core.util.RuntimeUtil
import cn.hutool.http.HtmlUtil
import cn.hutool.system.SystemUtil
import kotlinx.coroutines.runBlocking
import nekox.core.client.TdBot
import nekox.core.client.TdException
import nekox.core.raw.getChat
import nekox.core.raw.setLogVerbosityLevel
import nekox.core.toLink
import nekox.core.utils.makeFile
import java.io.File
import kotlin.system.exitProcess

object BuildScript {

    val workspace = "/github/workspace"

    @JvmStatic
    fun main(args: Array<String>) = runBlocking<Unit> {

        runCatching {

            TdLoader.tryLoad()

            setLogVerbosityLevel(1)

            val gitRef = SystemUtil.get("GITHUB_REF")
            val apkFile = File("$workspace/${SystemUtil.get("APK_FILE")}")
            val botToken = SystemUtil.get("TELEGRAM_TOKEN")
            val releaseChannel = SystemUtil.get("RELEASE_CHANNEL").toLong()
            val canaryChannel = SystemUtil.get("CANARY_CHANNEL").toLong()

            if(!apkFile.isFile) error("apk file not found")

            val bot = object : TdBot(botToken) {

                override suspend fun onAuthorizationFailed(ex: TdException) {

                    println("invalid bot token")

                    stop()

                    exitProcess(100)

                }

            }.apply {

                start()

                waitForAuth()

            }

            runCatching<Unit> {

                bot.getChat(releaseChannel)
                bot.getChat(canaryChannel)

                if (gitRef == "refs/heads/master") {

                    val gitHead = RuntimeUtil.getResult(RuntimeUtil.exec(arrayOf(), File(workspace), "git rev-parse HEAD"), CharsetUtil.CHARSET_UTF_8)
                    var commitLog = RuntimeUtil.getResult(RuntimeUtil.exec(arrayOf(), File(workspace), "git log -1"), CharsetUtil.CHARSET_UTF_8)

                    commitLog = HtmlUtil.escape(commitLog).replace(gitHead, gitHead.toLink("https://github.com/NekogramX/NekoX/commit/$gitHead"))

                    bot makeFile apkFile.path captionHtml commitLog syncTo canaryChannel

                } else if (gitRef == "refs/heads/release") {

                    val versionName = File("$workspace/TMessagesProj/build.gradle")
                            .readText()
                            .substringAfterLast("versionName")
                            .substringAfter("\"")
                            .substringBefore("\"")

                    var releaseMsg = "#v$versionName\n\n" + File("$workspace/whatsnew/whatsnew-zh-CN").readText()

                    val enNewFile = File("$workspace/whatsnew/whatsnew-en-US")

                    if (enNewFile.isFile) releaseMsg += "\n\n--------------------------\n\n" + enNewFile.readText()

                    bot makeFile apkFile.path captionHtml releaseMsg syncTo releaseChannel

                }

            }.onFailure {

                it.printStackTrace()

                bot.stop()

                exitProcess(100)

            }

            bot.stop()

            exitProcess(0)

        }.onFailure {

            it.printStackTrace()

            exitProcess(100)


        }

    }

}