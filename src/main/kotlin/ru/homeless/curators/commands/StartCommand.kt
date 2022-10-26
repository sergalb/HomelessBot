package ru.homeless.curators.commands

import mu.KotlinLogging
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import ru.homeless.database.Curator
import ru.homeless.database.Phone
import ru.homeless.database.curatorById
import ru.homeless.database.insertCurator
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
        val curator = curatorById(user.id)
        val answer = SendMessage()
        if (curator == null) {
            answer.setChatId(user.id)
            answer.text = MessageFormat.format(
                messageBundle.getProperty("curators.start.message"), user.firstName
            )

            answer.replyMarkup = keyboardWithContact()

            try {
                insertCurator(user.id, firstName = user.firstName, secondName = user.lastName)
                absSender.execute(answer)
            } catch (e: TelegramApiException) {
                logger.error { "Could not send start message because of ${e.message}" }
            }
        }
    }


    fun processNumberMessage(candidate: Curator, message: Message, absSender: AbsSender) {
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
            invalidPhoneMessage.text = messageBundle.getProperty("invalid.phone.format")
            try {
                absSender.execute(invalidPhoneMessage)
            } catch (e: TelegramApiException) {
                logger.error { "Error while invalid phone message send:  ${e.message}" }
            }
            return
        }

        candidate.updatePhone(normalizedPhone)

        val gratefulMessage = SendMessage()
        gratefulMessage.text = messageBundle.getProperty("thank.you.for.phone.curator")
        gratefulMessage.setChatId(message.chatId)
        gratefulMessage.replyMarkup = ReplyKeyboardRemove(true)
        try {
            absSender.execute(gratefulMessage)
        } catch (e: TelegramApiException) {
            logger.error { "Error while grateful message send: ${e.message} " }
        }
    }
}