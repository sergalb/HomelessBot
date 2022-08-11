package ru.homeless.curators.commands

import mu.KotlinLogging
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import ru.homeless.database.Curator
import ru.homeless.database.CuratorState
import ru.homeless.database.Phone
import ru.homeless.database.Roles
import ru.homeless.database.checkPermission
import ru.homeless.database.curatorById
import ru.homeless.database.findCuratorByPhone
import ru.homeless.database.updateCuratorStateById
import ru.homeless.messageBundle
import ru.homeless.sendMessage
import java.text.MessageFormat

object GrantAccessCommand : BotCommand("grant_access", "promote candidate to curator") {
    private val logger = KotlinLogging.logger {}

    override fun execute(absSender: AbsSender, user: User, chat: Chat, arguments: Array<out String>?) {
        val curator = curatorById(user.id)
        if (curator?.state != CuratorState.WAITING) {
            curator?.deleteLastMessage()
        }
        if (!checkPermission(
                user.id,
                absSender,
                Roles::hasRoleSpreadPermission,
                logger
            )

        ) return

        val answer = SendMessage()
        answer.setChatId(chat.id)
        answer.text = messageBundle.getString("provide.contact.to.grant")
        try {
            updateCuratorStateById(user.id, CuratorState.GRANT_ROLE)
            absSender.execute(answer)
        } catch (e: TelegramApiException) {
            logger.error { "Exception while send 'provide.contact.to.grant' message" }
        }
    }

    fun onCandidateContact(curator: Curator, message: Message, absSender: AbsSender) {
        if (!checkPermission(
                curator,
                absSender,
                message.chatId,
                Roles::hasMessagePermission,
                logger
            )
        ) return

        val (contact, role) = checkCandidateMessage(message, absSender) ?: return
        val candidate = Phone.byNumber(contact)?.let { findCuratorByPhone(it) }

        if (candidate == null) {
            absSender.sendMessage(messageBundle.getString("could.not.find.candidate.by.phone"), message.chatId) {
                logger.error { "Could not send could.not.find.candidate.by.phone message because of $it" }
            }
            logger.error { "Candidate on role grant is null" }
            return
        }

        val newRole = when (role) {
            "0" -> Roles.CANDIDATE
            "1" -> Roles.CURATOR
            "2" -> Roles.BOSS
            else -> {
                absSender.sendMessage(
                    messageBundle.getString("wrong.role.message"),
                    message.chatId
                )
                logger.error { "New role for grant access invalid: '$role'" }
                return
            }
        }

        val answer = SendMessage()
        answer.setChatId(message.chatId)
        answer.text = MessageFormat.format(
            messageBundle.getString("success.role.grant"),
            candidate.firstName,
            candidate.secondName ?: "",
            newRole.permissionDescription()
        )
        try {
            candidate.updateRole(newRole)
            curator.updateState(CuratorState.WAITING)
            absSender.execute(answer)
            absSender.sendMessage(
                messageBundle.getString("your.role.updated"),
                candidate.id.value
            )
        } catch (e: TelegramApiException) {
            logger.error { "Exception while sending 'success.role.grant' message." }
        }
    }

    private fun checkCandidateMessage(message: Message, absSender: AbsSender): Pair<String, String>? {
        val lines = message.text.lines()
        if (lines.size != 2) {
            val answer = SendMessage()
            answer.setChatId(message.from.id)
            answer.text = messageBundle.getString("invalid.candidate.promote.message")
            try {
                absSender.execute(answer)
            } catch (e: TelegramApiException) {
                logger.error { "Exception while send 'invalid.candidate.promote.message' message." }
            }
            return null
        }
        return lines.first() to lines.last()
    }
}
