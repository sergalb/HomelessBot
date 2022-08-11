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
import ru.homeless.database.insertOrUpdateMessage
import ru.homeless.database.isStatusExistForVolunteers
import ru.homeless.messageBundle
import ru.homeless.sendMessage
import java.text.MessageFormat


object OnStatusUpdateCommand :
    BotCommand("on_status_update", "allow create send message when volunteer's status update in spreadsheet ") {
    private val logger = KotlinLogging.logger {}

    //todo make in divided steps
    override fun execute(absSender: AbsSender, user: User, chat: Chat, arguments: Array<out String>?) {
        val curator = curatorById(user.id)
        if (curator?.state != CuratorState.WAITING) {
            curator?.deleteLastMessage()
        }
        if (curator != null) {
            curator.updateState(CuratorState.REQUEST_ON_UPDATE)
            absSender.sendMessage(
                messageBundle.getString("request.on.status.update"),
                user.id
            ) { logger.error { "Could not send request.on.status.update message because of ${it.message}" } }
        }
    }

    fun processUpdateSetup(curator: Curator, message: Message, absSender: AbsSender) {
        if (!checkPermission(
                curator,
                absSender,
                message.chatId,
                Roles::hasMessagePermission,
                logger
            )
        ) return

        val messageLines = message.text.lines()
        if (messageLines.size < 3) {
            absSender.sendMessage(messageBundle.getString("update.message.should.contain.3.lines"), message.chatId)
            return
        }

        val oldStatus = messageLines.first().trim()
        val newStatus = messageLines[1].trim()
        if (!isStatusExistForVolunteers(oldStatus)) {
            absSender.sendMessage(
                MessageFormat.format(messageBundle.getString("volunteers.with.status.does.not.exist"), "старым"),
                message.chatId
            )
        }

        if (!isStatusExistForVolunteers(newStatus)) {
            absSender.sendMessage(
                MessageFormat.format(messageBundle.getString("volunteers.with.status.does.not.exist"), "новым"),
                message.chatId
            )
        }
        val text = messageLines.drop(2).joinToString(separator = "\n")

        insertOrUpdateMessage(oldStatus, newStatus, text)
        curator.updateState(CuratorState.WAITING)

        absSender.sendMessage(
            MessageFormat.format(messageBundle.getString("update.message.created"), oldStatus, newStatus),
            message.chatId
        ) { logger.error { "Could not send message update.message.created because of ${it.message}" } }
    }

}