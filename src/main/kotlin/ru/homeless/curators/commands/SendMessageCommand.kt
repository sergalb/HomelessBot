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
import ru.homeless.curators.MessageCreator
import ru.homeless.database.Curator
import ru.homeless.database.CuratorState
import ru.homeless.database.Roles
import ru.homeless.database.Volunteer
import ru.homeless.database.checkPermission
import ru.homeless.database.findVolunteerByPhone
import ru.homeless.database.personByContact
import ru.homeless.database.updateCuratorStateById
import ru.homeless.messageBundle
import ru.homeless.volunteers.volunteersBot
import java.text.MessageFormat

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
            val volunteer = personByContact(contact, ::findVolunteerByPhone)
            if (volunteer != null) {
                volunteers.add(volunteer)
            } else {
                logger.error { "Could not found volunteer by contact $contact" }
                notFoundContacts.add(contact)
            }
        }

        val distinctVolunteer = volunteers.distinctBy { it.id.value }
        if (distinctVolunteer.size != volunteers.size) {
            //todo send response to curator
            logger.warn { "Found duplicate of volunteers for send message" }
        }


        val answer = SendMessage()
        answer.setChatId(message.chatId)
        answer.text = confirmationMessage(curator, distinctVolunteer)

        //todo add confirmation keyboard
//        answer.replyMarkup = confirmationKeyboard()

        if (notFoundContacts.isNotEmpty()) {
            logger.error { "Could not find contacts $notFoundContacts" }
            //todo send response to curator with not founded volunteers contacts
        }

        try {
            lastCuratorMessage.addVolunteersToMessage(distinctVolunteer)
            //todo implement schedule part
            curator.updateState(CuratorState.SCHEDULE_MESSAGE)
            absSender.execute(answer)
        } catch (e: TelegramApiException) {
            logger.error { "Exception while send 'curator.take.contacts' message to ${curator.firstName} ${curator.secondName} because of ${e.message}" }
        } catch (e: RuntimeException) {
            logger.error { "Could not recieve volunteers or send message" }
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
        TODO("Not yet implemented")
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

        val answer = SendMessage()
        answer.setChatId(message.chatId)
        answer.text = messageBundle.getString("confirm.message")
        val curatorMessage = curator.lastMessage()
        if (curatorMessage == null ) {
            //todo add description
            return
        }

        curator.updateState(CuratorState.WAITING)
        runBlocking {
            volunteersBot.receive(curatorMessage.text, curatorMessage.volunteersAsList().map { it.id.value })
        }
        try {
            absSender.execute(answer)
        } catch (e: TelegramApiException) {
            logger.error { "Exception while send 'curator.take.contacts' message to ${curator.firstName} ${curator.secondName} because of ${e.message}" }
        }
    }

    private fun confirmationMessage(curator: Curator, volunteers: List<Volunteer>): String {
        val message = curator.lastMessage()?.text
        return MessageFormat.format(
            messageBundle.getString("curator.take.contacts"),
            volunteers.size,
            message,
            volunteers.size,
            "сейчас"
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
