package ru.homeless.curators.commands

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import ru.homeless.curators.MessageCreator
import ru.homeless.database.Curator
import ru.homeless.database.CuratorState
import ru.homeless.database.Roles
import ru.homeless.database.Volunteer
import ru.homeless.database.checkPermission
import ru.homeless.database.curatorById
import ru.homeless.database.findVolunteerByContact
import ru.homeless.database.updateCuratorStateById
import ru.homeless.messageBundle
import ru.homeless.sendMessage
import ru.homeless.volunteers.volunteersBot
import java.text.MessageFormat
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object SendMessageCommand : BotCommand("send_message", "send message to volunteers"), MessageCreator {
    private val logger = KotlinLogging.logger {}

    override fun execute(absSender: AbsSender, user: User, chat: Chat, arguments: Array<out String>?) {
        if (!checkPermission(
                user.id,
                absSender,
                Roles::hasMessagePermission,
                logger
            )
        ) return
        val curator = curatorById(user.id)
        if (curator?.state != CuratorState.WAITING) {
            curator?.deleteLastMessage()
        }
        val answer = SendMessage()
        answer.setChatId(user.id)
        answer.text = messageBundle.getString("request.message.text")
        try {
            absSender.execute(answer)
            updateCuratorStateById(user.id, CuratorState.REQUEST_MESSAGE)
        } catch (e: TelegramApiException) {
            logger.error { "Exception while send message request to curator ${user.firstName} ${user.lastName}, because of ${e.message}" }
        }
    }

    override fun onText(curator: Curator, message: Message, absSender: AbsSender) {
        if (!checkPermission(
                curator,
                absSender,
                message.chatId,
                Roles::hasMessagePermission,
                logger
            )
        ) return

        curator.updateMessageAndState(message.text, CuratorState.SEND_MESSAGE)

        val answer = SendMessage()
        answer.setChatId(message.chatId)
        answer.text = messageBundle.getString("curator.send.message")
        try {
            absSender.execute(answer)
        } catch (e: TelegramApiException) {
            logger.error { "Exception while send 'curator.send.message' response because of ${e.message}" }
        }
    }

    override fun onVolunteers(curator: Curator, message: Message, absSender: AbsSender) {
        if (!checkPermission(
                curator,
                absSender,
                message.chatId,
                Roles::hasMessagePermission,
                logger
            )
        ) return

        val lastCuratorMessage = curator.lastMessage()
        if (lastCuratorMessage == null) {
            logger.error { "Could not find message for curator ${curator.firstName} ${curator.secondName}" }
            return
        }

        val volunteers = mutableListOf<Volunteer>()
        val notFoundContacts = mutableListOf<String>()
        for (contact in message.text.lineSequence()) {
            val volunteer = findVolunteerByContact(contact)
            if (volunteer != null) {
                volunteers.add(volunteer)
            } else {
                notFoundContacts.add(contact)
            }
        }

        val groupedVolunteers = volunteers.groupBy { it.id.value }
        if (groupedVolunteers.any { it.value.size > 1 }) {
            val duplicates = groupedVolunteers
                .filter { it.value.size > 1 }
                .map { it.value.first() }
                .joinToString(separator = "\n") { "${it.firstName}  ${it.secondName ?: ""}" }

            absSender.sendMessage(
                MessageFormat.format(messageBundle.getString("duplicated.volunteers"), duplicates),
                message.chatId
            )

            logger.warn { "Found duplicate of volunteers for send message, duplicates: $duplicates" }
        }

        if (notFoundContacts.isNotEmpty()) {
            absSender.sendMessage(
                MessageFormat.format(
                    messageBundle.getString("not.found.contacts"),
                    notFoundContacts.joinToString(separator = "\n")
                ),
                message.chatId
            )
            logger.error { "Could not find contacts $notFoundContacts" }
        }

        val volunteersToSend = groupedVolunteers.mapNotNull { it.value.firstOrNull() }
        if (volunteersToSend.isEmpty()) {
            absSender.sendMessage(messageBundle.getString("not.found.volunteers.to.send.message"), message.chatId)
            curator.deleteLastMessage()
            return
        }
        lastCuratorMessage.addVolunteersToMessage(volunteersToSend)
        curator.updateState(CuratorState.SEND_PHONE_OR_EMAIL_OR_STATUS)

        //todo add inline keyboard via process callback data
        val nowKeyboard = InlineKeyboardMarkup.builder().keyboard(
            listOf(
                listOf(
                    InlineKeyboardButton
                        .builder()
                        .text(messageBundle.getString("now"))
                        .callbackData(messageBundle.getString("now"))
                        .build()
                )
            )
        ).build()

        absSender.sendMessage(
            MessageFormat.format(messageBundle.getString("curator.take.contacts"), groupedVolunteers.size),
            message.chatId
//            replyKeyboard = nowKeyboard
        ) {
            logger.error { "Exception while send 'curator.take.contacts' message to ${curator.firstName} ${curator.secondName} because of ${it.message}" }
        }


    }

    override fun onSchedule(curator: Curator, message: Message, absSender: AbsSender) {
        if (!checkPermission(
                curator,
                absSender,
                message.chatId,
                Roles::hasMessagePermission,
                logger
            )
        ) return

        val lastCuratorMessage = curator.lastMessage()
        if (lastCuratorMessage == null) {
            logger.error { "Could not find message for curator ${curator.firstName} ${curator.secondName ?: ""}" }
            return
        }

        try {
            val date: LocalDateTime = if (message.text.lowercase() == "сейчас") {
                LocalDateTime.now(ZoneId.of("Europe/Moscow"))
            } else {
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")
                LocalDateTime.parse(message.text, formatter).atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime()
            }
            lastCuratorMessage.addScheduleToMessage(date)
            curator.updateState(CuratorState.SCHEDULE_MESSAGE)

            //todo add confirmation keyboard
//        answer.replyMarkup = confirmationKeyboard()

            absSender.sendMessage(
                confirmationMessage(curator),
                message.chatId
            )
        } catch (e: DateTimeException) {
            absSender.sendMessage(
                MessageFormat.format(messageBundle.getString("incorrect.datetime.format"), e.message),
                message.chatId
            )
        }
    }

    override fun onConfirmation(curator: Curator, message: Message, absSender: AbsSender) {
        if (!checkPermission(
                curator,
                absSender,
                message.chatId,
                Roles::hasMessagePermission,
                logger
            )
        ) return

        if (message.text.lowercase() != "да") {
            absSender.sendMessage(
                messageBundle.getString("dont.confirm.retry"),
                message.chatId
            )
            curator.deleteLastMessage()
            return
        }
        val curatorMessage = curator.lastMessage()
        if (curatorMessage == null) {
            logger.error { "Could not find message for curator" }
            return
        }

        curator.updateState(CuratorState.WAITING)
        val volunteers = curatorMessage.volunteersAsList()
        val date = curatorMessage.schedule ?: LocalDateTime.now().atZone(ZoneId.of("Europe/Moscow")).toLocalDateTime()
        runBlocking {
            volunteersBot.receive(curatorMessage.text, volunteers.map { it.id.value }, date) { ind, e ->
                var errorDescription = MessageFormat.format(
                    messageBundle.getString("could.not.send.message.to.volunteer"),
                    volunteers[ind].firstName,
                    volunteers[ind].secondName ?: ""
                )
                if (e is TelegramApiRequestException &&
                    e.errorCode == 400 &&
                    e.apiResponse == "Bad Request: chat not found"
                ) {
                    errorDescription += messageBundle.getString("volunteers.not.start.bot")
                }
                absSender.sendMessage(errorDescription, message.chatId) {
                    logger.error { "Could not send could.not.send.message.to.volunteer because of ${it.message}" }
                }
            }
        }
        curator.deleteLastMessage()

        absSender.sendMessage(
            messageBundle.getString("confirm.message"),
            message.chatId
        ) {
            logger.error { "Exception while send 'confirm.message' message to ${curator.firstName} ${curator.secondName} because of ${it.message}" }
        }
    }

    private fun confirmationMessage(curator: Curator): String {
        val message = curator.lastMessage()
        return MessageFormat.format(
            messageBundle.getString("curator.take.schedule"),
            message?.text ?: "",
            message?.volunteersAsList()?.size ?: "",
            message?.schedule?.format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm", Locale("ru")))
        )
    }

    private fun confirmationKeyboard(): InlineKeyboardMarkup? {
        return InlineKeyboardMarkup.builder().keyboardRow(
            listOf(
                InlineKeyboardButton("Подтвердить"),
                InlineKeyboardButton("Что-то поменять")
            )
        ).build()
    }
}
