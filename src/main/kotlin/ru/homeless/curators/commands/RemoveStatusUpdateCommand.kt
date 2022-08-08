package ru.homeless.curators.commands

import mu.KotlinLogging
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.bots.AbsSender
import ru.homeless.database.Curator
import ru.homeless.database.CuratorState
import ru.homeless.database.Roles
import ru.homeless.database.checkPermission
import ru.homeless.database.curatorById
import ru.homeless.database.deleteMessagesWithStatuses
import ru.homeless.messageBundle
import ru.homeless.sendMessage
import java.text.MessageFormat

object RemoveStatusUpdateCommand :
    BotCommand("remove_status_update", "allow remove send message when volunteer's status update in spreadsheet ") {
    private val logger = KotlinLogging.logger {}

    override fun execute(absSender: AbsSender, user: User, chat: Chat, arguments: Array<out String>?) {
        val curator = curatorById(user.id)
        if (curator != null) {
            curator.updateState(CuratorState.REQUEST_REMOVE_UPDATE)
            absSender.sendMessage(
                messageBundle.getString("request.remove.status.update"),
                user.id
            ) { logger.error { "Could not send request.remove.status.update message because of ${it.message}" } }
        }
    }

    fun processRemoveUpdate(curator: Curator, message: Message, absSender: AbsSender) {
        if (!checkPermission(
                curator,
                absSender,
                message.chatId,
                Roles::hasMessagePermission,
                logger
            )
        ) return

        val messageLines = message.text.lines()
        if (messageLines.size != 2) {
            absSender.sendMessage(
                messageBundle.getString("remove.update.message.should.contain.2.lines"),
                message.chatId
            )
            return
        }

        val oldStatus = messageLines.first().trim()
        val newStatus = messageLines.last().trim()

        val countDeleted = deleteMessagesWithStatuses(oldStatus, newStatus)
        if (countDeleted == 0) {
            absSender.sendMessage(messageBundle.getString("could.not.find.update.message"), message.chatId) {
                logger.error { "Could not send could.not.find.update.message because of ${it.message}" }
            }
        } else {
            curator.updateState(CuratorState.WAITING)
            absSender.sendMessage(
                MessageFormat.format(messageBundle.getString("update.message.deleted"), oldStatus, newStatus),
                message.chatId
            ) { logger.error { "Could not send message update.message.deleted because of ${it.message}" } }
        }
    }
}