package ru.homeless.curators.commands

import mu.KotlinLogging
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.bots.AbsSender
import ru.homeless.database.Roles
import ru.homeless.database.checkPermission
import ru.homeless.database.curatorById
import ru.homeless.database.getVolunteersCount
import ru.homeless.database.selectAllMessages
import ru.homeless.messageBundle
import ru.homeless.sendMessage
import java.text.MessageFormat

object CheckVolunteersCount : BotCommand("get_volunteers_count", "get count of volunteers working with bot") {
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

        absSender.sendMessage(
            MessageFormat.format(
                messageBundle.getProperty("volunteers.count"),
                getVolunteersCount()
            ),
            chat.id) { e ->
            logger.error { "Could not send volunteers.count messages because of ${e.message}" }
        }
    }
}