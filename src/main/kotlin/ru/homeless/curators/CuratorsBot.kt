package ru.homeless.curators

import mu.KotlinLogging
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot
import org.telegram.telegrambots.meta.api.objects.Update
import ru.homeless.curators.commands.GrantAccessCommand
import ru.homeless.curators.commands.OnStatusUpdateCommand
import ru.homeless.curators.commands.RemoveStatusUpdateCommand
import ru.homeless.curators.commands.SendMessageCommand
import ru.homeless.curators.commands.StartCommand
import ru.homeless.database.CuratorState
import ru.homeless.database.curatorById
import ru.homeless.messageBundle
import ru.homeless.sendMessage


val curatorsBot = object : TelegramLongPollingCommandBot() {
    private val logger = KotlinLogging.logger {}

    init {
        register(StartCommand)
        register(SendMessageCommand)
        register(GrantAccessCommand)
        register(OnStatusUpdateCommand)
        register(RemoveStatusUpdateCommand)
        registerDefaultAction { absSender, message ->
            absSender.sendMessage(messageBundle.getString("unknown.command"), message.chatId)
            logger.info {
                """Unknown command curator: 
                    volunteer: ${message.chatId},
                    text: ${message.text}""".trimIndent()
            }
        }
    }

    private val curatorsBotToken: String by lazy { ru.homeless.getLocalProperty("curators_bot_token") }

    override fun getBotToken() = curatorsBotToken

    override fun getBotUsername() = "Бот кураторов Ночлежки"

    override fun processNonCommandUpdate(update: Update) {
        val updateMessage = update.message
        val curator = curatorById(updateMessage.from.id)
        when (curator?.state) {
            CuratorState.START -> StartCommand.processNumberMessage(curator, updateMessage, this)
            CuratorState.REQUEST_MESSAGE -> SendMessageCommand.onText(curator, updateMessage, this)
            CuratorState.SEND_MESSAGE -> SendMessageCommand.onVolunteers(curator, updateMessage, this)
            CuratorState.SEND_PHONE_OR_EMAIL_OR_STATUS -> SendMessageCommand.onSchedule(curator, updateMessage, this)
            CuratorState.SCHEDULE_MESSAGE -> SendMessageCommand.onConfirmation(curator, updateMessage, this)
            CuratorState.GRANT_ROLE -> GrantAccessCommand.onCandidateContact(curator, updateMessage, this)
            CuratorState.REQUEST_ON_UPDATE -> OnStatusUpdateCommand.processUpdateSetup(curator, updateMessage, this)
            CuratorState.REQUEST_REMOVE_UPDATE -> RemoveStatusUpdateCommand.processRemoveUpdate(curator, updateMessage, this)
            else -> logger.error { "Could not find curator state for ${updateMessage.from.firstName} ${updateMessage.from.lastName}. State: ${curator?.state}" }
        }
    }

}