package ru.homeless.volunteers

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.bots.AbsSender
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds

data class Message(
    val text: String,
    val id: Long,
    var attempt: Int = 0,
    val onError: (Exception) -> Unit
)

@OptIn(DelicateCoroutinesApi::class)
class MessageQueueImpl(private val absSender: AbsSender) : MessageQueue {
    private val logger = KotlinLogging.logger {}
    private val queue = Channel<Message>(1024)
    private val DELAY = 1.seconds / 10
    private val ATTEMPTS_COUNT = 3

    init {
        GlobalScope.launch {
//            var last = System.currentTimeMillis().milliseconds
            try {
                for (message: Message in queue) {
                    process(message)
//                val cur = System.currentTimeMillis().milliseconds
                    logger.info { "${message.text}, ${message.id}" }//, time between messages ${(cur - last).inWholeMilliseconds}ms" }
//                last = cur
                    delay(DELAY)
                }
            } catch (e: Exception) {
                logger.error { "Exception on daemon queue run " }
            }

        }
    }

    private suspend fun process(message: Message) {
        val telegramMessage = SendMessage()
        telegramMessage.text = message.text
        telegramMessage.setChatId(message.id)
        try {
            absSender.execute(telegramMessage)
        } catch (e: Exception) {
            logger.error { "Could not send message to chat ${message.id} because of ${e.message}, attempt: ${message.attempt}" }
            if (message.attempt >= ATTEMPTS_COUNT) {
                message.onError(e)
            } else {
                queue.send(message.apply { attempt++ })
            }
        }
    }

    override suspend fun sendMessage(text: String, id: Long, onError: (Exception) -> Unit) =
        withContext(Dispatchers.Default) {
            queue.send(Message(text, id, 0, onError))
        }

    override suspend fun scheduleMessage(text: String, id: Long, date: LocalDateTime, onError: (Exception) -> Unit) {
        GlobalScope.launch {
            try {
                val now = LocalDateTime.now(ZoneId.of("Europe/Moscow"))
                    .toEpochSecond(ZoneOffset.ofTotalSeconds(0))
                val dateInSeconds = date.toEpochSecond(ZoneOffset.ofTotalSeconds(0))
                delay((dateInSeconds - now).seconds)
                sendMessage(text, id, onError)
            } catch (e: Exception) {
                logger.error { "Error during message scheduling" }
            }
        }

    }

}