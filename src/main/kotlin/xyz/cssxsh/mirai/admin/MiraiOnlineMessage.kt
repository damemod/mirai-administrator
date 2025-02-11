package xyz.cssxsh.mirai.admin

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import net.mamoe.mirai.*
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.code.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import xyz.cssxsh.mirai.admin.data.*
import xyz.cssxsh.mirai.spi.*
import java.util.*
import kotlin.collections.*

public object MiraiOnlineMessage : BotOnlineAction, MiraiOnlineMessageConfig by AdminOnlineMessageConfig {
    override val level: Int = 0
    override val id: String = "online"

    private val cache: MutableSet<Long> = HashSet()

    @OptIn(MiraiExperimentalApi::class)
    private suspend fun xml(group: Group) = buildXmlMessage(1) {
        templateId = -1
        action = "web"
        brief = "QQ Bot 已启动"
        flag = 0

        val avatar = avatars.getOrPut(group.bot.avatarUrl) { http.get(group.bot.avatarUrl).body() }
        val image = avatar.toExternalResource().use { it.uploadAsImage(group) }

        item {
            layout = 2
            picture(coverUrl = image.queryUrl())
            title(text = "[${group.botAsMember.nick}]已加入该会话")
            summary(text = "[${group.botAsMember.nick}]开始接受指令执行")
        }

        source(name = "QQ Bot 已启动，可以开始执行指令")
    }

    private fun plain(group: Group) = buildMessageChain {
        appendLine("[${group.botAsMember.nick}]已加入该会话")
        appendLine("[${group.botAsMember.nick}]开始接受指令执行")
    }

    private val avatars: MutableMap<String, ByteArray> = WeakHashMap()

    override suspend fun run(bot: Bot) {
        if (cache.add(bot.id)) return
        for (group in bot.groups) {
            if (!permission.testPermission(group.permitteeId)) continue

            val message = when (type) {
                MiraiOnlineMessageConfig.Type.XML -> xml(group = group)
                MiraiOnlineMessageConfig.Type.PLAIN -> plain(group = group)
                MiraiOnlineMessageConfig.Type.CUSTOM -> MiraiCode.deserializeMiraiCode(code = custom, contact = group)
            }
            try {
                group.sendMessage(message = message)
            } catch (_: Throwable) {
                //
            }
            delay(duration * 1_000)
        }
    }
}