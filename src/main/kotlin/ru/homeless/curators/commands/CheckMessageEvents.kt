package ru.homeless.curators.commands

import mu.KotlinLogging
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.bots.AbsSender
import ru.homeless.database.Roles
import ru.homeless.database.checkPermission
import ru.homeless.database.curatorById
import ru.homeless.database.selectAllMessages
import ru.homeless.messageBundle
import ru.homeless.sendMessage
import java.text.MessageFormat

object CheckMessageEvents : BotCommand("get_message_events", "get list of message sending by changed status") {
    private val logger = KotlinLogging.logger {}
    override fun execute(absSender: AbsSender, user: User, chat: Chat, arguments: Array<out String>?) {
        val curator = curatorById(user.id)
        if (!checkPermission(
                curator,
                absSender,
                chat.id,
                Roles::hasMessagePermission,
                logger
            )
        ) return

        val events = selectAllMessages()
        absSender.sendMessage(
            MessageFormat.format(
                messageBundle.getProperty("all.events"),
                events.withIndex().joinToString(separator = "\n\n") { "#${it.index + 1} ${it.value}"}
            ),
            chat.id) { e ->
            logger.error { "Could not send all.events messages because of ${e.message}" }
        }
    }
}