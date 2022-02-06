package xyz.cssxsh.mirai.plugin

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import net.mamoe.mirai.*
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.console.util.ContactUtils.render
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import xyz.cssxsh.mirai.plugin.data.*
import xyz.cssxsh.mirai.spi.*
import java.util.*

internal inline val logger get() = MiraiAdminPlugin.logger

internal fun Bot.owner() = getFriendOrFail(AdminSetting.owner)

internal val http by lazy { HttpClient(OkHttp) }

internal fun AbstractJvmPlugin.registerPermission(name: String, description: String): Permission {
    return PermissionService.INSTANCE.register(permissionId(name), description, parentPermission)
}

internal fun NewFriendRequestEvent.render(accept: Boolean): Message = buildMessageChain {
    appendLine("@${fromNick}#${fromId} with <${eventId}>")
    appendLine("申请添加好友")
    appendLine("from $fromGroup")
    appendLine(message)
    if (accept) appendLine("已自动同意")
}

internal fun MemberJoinRequestEvent.render(accept: Boolean): Message = buildMessageChain {
    appendLine("@${fromNick}#${fromId} with <${eventId}>")
    appendLine("申请加入群")
    appendLine("to [$groupName](${groupId}) by $invitorId")
    appendLine(message)
    if (accept) appendLine("已自动同意")
}

internal fun BotInvitedJoinGroupRequestEvent.render(accept: Boolean): Message = buildMessageChain {
    appendLine("@${invitorNick}#${invitorId} with <${eventId}>")
    appendLine("邀请机器人加入群")
    appendLine("to [${groupName}](${groupId})")
    if (accept) appendLine("已自动同意")
}

internal fun AdminRequestEventData.render(): String = buildString {
    for ((qq, list) in this@render) {
        if (list.isEmpty()) continue
        val bot = try {
            Bot.getInstance(qq).render()
        } catch (_: Throwable) {
            "$qq"
        }
        appendLine("--- $bot ---")
        for (request in list) {
            appendLine(request)
        }
    }
    if (isEmpty()) {
        appendLine("没有记录")
    }
}

internal fun ComparableService.Loader.reload() {
    for (classLoader in JvmPluginLoader.classLoaders) {
        instances.addAll(ServiceLoader.load(ComparableService::class.java, classLoader))
        for (subclass in ComparableService::class.sealedSubclasses) {
            instances.addAll(ServiceLoader.load(subclass.java, classLoader))
        }
    }
    instances.add(MiraiAutoApprover)
    instances.add(MiraiOnlineMessage)
    instances.add(MiraiStatusMessage)
    instances.add(MiraiMemberCleaner)
    instances.add(MiraiCurfewTimer)
    instances.add(MiraiContentCensor)
    if (invoke<MessageSourceHandler>().isEmpty()) {
        instances.add(MiraiMessageRecorder)
    }
}

internal fun ComparableService.Loader.render(): String = buildString {
    appendLine("ComparableService Registered [${instances.size}]:")
    for (subclass in ComparableService::class.sealedSubclasses) {
        appendLine("${subclass.simpleName}: ${registered(subclass.java).joinToString { "${it.id}(${it.level})" }}")
    }
}