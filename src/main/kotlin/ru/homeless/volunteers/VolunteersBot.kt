package ru.homeless.volunteers

import mu.KotlinLogging
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot
import org.telegram.telegrambots.meta.api.objects.Update
import ru.homeless.database.VolunteerState
import ru.homeless.database.volunteersStateById
import ru.homeless.messageBundle
import ru.homeless.sendMessage
import java.time.LocalDateTime

val volunteersBot = VolunteersBoot

object VolunteersBoot : TelegramLongPollingCommandBot(), MessageReceiver {
    private val logger = KotlinLogging.logger {}
    private val messageQueue: MessageQueue = MessageQueueImpl(this)
    private val startCommand = StartCommand

    init {
        register(startCommand)
        registerDefaultAction { absSender, message ->
            absSender.sendMessage(messageBundle.getProperty("unknown.command"), message.chatId)
            logger.info {
                """Unknown command volunteer: 
                    user: ${message.chatId},
                    text: ${message.text}""".trimIndent()
            }
        }
    }

    private val volunteersBotToken: String by lazy { ru.homeless.getLocalProperty("volunteers_bot_token") }

    override fun getBotToken(): String = volunteersBotToken

    override fun getBotUsername() = "Бот волонтеров Ночлежкиr"

    override fun processNonCommandUpdate(update: Update?) {
        val message = update?.message ?: return
        val state = volunteersStateById(message.chatId)
        if (state == VolunteerState.STARTED) {
            startCommand.processNumberMessage(message, this)
        } else {
            logger.warn {
                """Get message from volunteer in state ${state?.toString() ?: "unstarted"}. 
                    Message: ${message.text}""".trimIndent()
            }
        }
    }

    override suspend fun receive(
        text: String,
        ids: List<Long>,
        date: LocalDateTime,
        onError: (Int, Exception) -> Unit
    ) {
        ids.forEachIndexed { ind, id ->
            messageQueue.scheduleMessage(text, id, date) { onError(ind, it) }
        }
    }

    override suspend fun receive(text: String, ids: List<Long>, onError: (Int, Exception) -> Unit) {
        ids.forEachIndexed { ind, id ->
            messageQueue.sendMessage(text, id) { onError(ind, it) }
        }
    }
}