package ru.homeless.volunteers

import mu.KotlinLogging
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot
import org.telegram.telegrambots.meta.api.objects.Update
import ru.homeless.database.VolunteerState
import ru.homeless.database.volunteersStateById

val volunteersBot = VolunteersBoot

object VolunteersBoot : TelegramLongPollingCommandBot(), MessageReceiver {
    private val logger = KotlinLogging.logger {}
    private val messageQueue: MessageQueue = MessageQueueImpl(this)
    private val startCommand = StartCommand

    init {
        register(startCommand)
        registerDefaultAction { absSender, message ->
            //todo help message
            logger.info {
                """Unknown command used: 
                    user: ${message.chatId},
                    text: ${message.text}""".trimIndent()
            }
        }
    }

    private val volunteersBotToken: String by lazy { ru.homeless.getBotToken("volunteers_bot_token") }

    override fun getBotToken(): String = volunteersBotToken

    override fun getBotUsername() = "homeless_volunteers"

    override fun processNonCommandUpdate(update: Update?) {
        val message = update?.message ?: return
        val state = volunteersStateById(message.chatId)
        if (state == VolunteerState.STARTED) {
            startCommand.processNumberMessage(message, this)
        } else {
            //todo add warn to user; is it safe print user message?
            logger.warn {
                """Get message from volunteer in state ${state?.toString() ?: "unstarted"}. 
                    Message: ${message.text}""".trimIndent()
            }
        }
    }

    override suspend fun receive(text: String, ids: List<Long>) {
        ids.forEach { messageQueue.sendMessage(text, it) }
    }
}