package ru.homeless.volunteers

import mu.KotlinLogging
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import ru.homeless.database.State
import ru.homeless.database.insertVolunteer
import ru.homeless.database.volunteersStateById
import ru.homeless.messageBundle
import java.text.MessageFormat

class StartCommand : BotCommand("start", "start conversation with user") {
    private val logger = KotlinLogging.logger {}
    override fun execute(absSender: AbsSender?, user: User?, chat: Chat?, arguments: Array<out String>?) {
        if (chat == null || user == null) {
            if (chat == null) logger.error { "Null chat found" }
            if (user == null) logger.error { "Null user found" }
            return
        }
        if (!chat.isUserChat) {
            logger.error { "Call start command from non user chat ${chat.firstName} ${chat.lastName}" }
            return
        }
        val volunteerState = volunteersStateById(user.id)
        val answer = SendMessage()
        if (volunteerState == null) {
            answer.setChatId(chat.id)
            answer.text = MessageFormat.format(
                messageBundle.getString("volunteers.start.message"), user.firstName
            )

            //todo rework size of button; make it one time appear
            val keyboard = ReplyKeyboardMarkup(
                listOf(
                    KeyboardRow(
                        listOf(KeyboardButton(messageBundle.getString("get.phone.keyboard"), true, false, null, null))
                    )
                )
            )

            answer.replyMarkup = keyboard
            insertVolunteer(user.id, firstName = user.firstName, secondName = user.lastName, state = State.STARTED)

            try {
                absSender?.execute(answer) ?: logger.error { "Does not get absSsender" }
            } catch (e: TelegramApiException) {
                logger.error { "Could not send start message because of ${e.message}" }
            }
        }

    }
}