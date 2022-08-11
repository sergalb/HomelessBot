package ru.homeless.volunteers

import mu.KotlinLogging
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import ru.homeless.database.Phone
import ru.homeless.database.VolunteerState
import ru.homeless.database.insertVolunteer
import ru.homeless.database.updatePhoneAndState
import ru.homeless.database.updateVolunteer
import ru.homeless.database.volunteersStateById
import ru.homeless.google.findUserByPhone
import ru.homeless.keyboardWithContact
import ru.homeless.messageBundle
import java.text.MessageFormat


object StartCommand : BotCommand("start", "start conversation with user") {
    private val logger = KotlinLogging.logger {}
    override fun execute(absSender: AbsSender, user: User, chat: Chat, arguments: Array<out String>?) {
        if (!chat.isUserChat) {
            logger.error { "Call start command from non user chat ${chat.firstName} ${chat.lastName}" }
            return
        }
        val volunteerState = volunteersStateById(user.id)
        val answer = SendMessage()
        if (volunteerState == null) {
            answer.setChatId(user.id)
            answer.text = MessageFormat.format(
                messageBundle.getString("volunteers.start.message"), user.firstName
            )
            answer.replyMarkup = keyboardWithContact()

            try {
                insertVolunteer(
                    user.id, firstName = user.firstName, secondName = user.lastName, state = VolunteerState.STARTED
                )
                absSender.execute(answer)
            } catch (e: TelegramApiException) {
                logger.error { "Could not send start message because of ${e.message}" }
            }
        }
    }

    fun processNumberMessage(message: Message, absSender: AbsSender) {
        val normalizedPhone = Phone.byNumber(
            if (message.hasContact()) {
                message.contact.phoneNumber
            } else {
                message.text
            }
        )

        if (normalizedPhone == null) {
            val invalidPhoneMessage = SendMessage()
            invalidPhoneMessage.setChatId(message.chatId)
            invalidPhoneMessage.text = messageBundle.getString("invalid.phone.format")
            try {
                absSender.execute(invalidPhoneMessage)
            } catch (e: TelegramApiException) {
                logger.error { "Error while invalid phone message send:  ${e.message}" }
            }
            return
        }

        val spreadsheetVolunteer = findUserByPhone(normalizedPhone)
        if (spreadsheetVolunteer != null) {
            updateVolunteer(message.chatId, spreadsheetVolunteer, normalizedPhone, VolunteerState.IDENTIFIED)
        } else {
            updatePhoneAndState(message.chatId, normalizedPhone, VolunteerState.IDENTIFIED)
        }

        val gratefulMessage = SendMessage()
        gratefulMessage.text = messageBundle.getString("thank.you.for.phone")
        gratefulMessage.setChatId(message.chatId)
        gratefulMessage.replyMarkup = ReplyKeyboardRemove(true)
        try {
            absSender.execute(gratefulMessage)
        } catch (e: TelegramApiException) {
            logger.error { "Error while grateful message send: ${e.message} " }
        }
    }

}