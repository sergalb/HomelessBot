package ru.homeless.volunteers

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


@OptIn(DelicateCoroutinesApi::class)
class MessageQueueImpl(private val absSender: AbsSender) : MessageQueue {
    private val logger = KotlinLogging.logger {}
    private val queue = Channel<Pair<String, Long>>(Channel.UNLIMITED)
    private val DELAY = 1.seconds / 10

    init {
        GlobalScope.launch {
            var last = System.currentTimeMillis().milliseconds
            for ((text, id) in queue) {
                process(text, id)
                val cur = System.currentTimeMillis().milliseconds
                logger.info { "$text, $id, time between messages ${(cur - last).inWholeMilliseconds}ms" }
                last = cur
                delay(DELAY )
            }

        }
    }

    private suspend fun process(text: String, id: Long) {
        val message = SendMessage()
        message.text = text
        message.setChatId(id)
        try {
            absSender.execute(message)
        } catch (e: TelegramApiException) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            logger.error { "Could not send message to chat $id because of $sw" }
            sendMessage(text, id)
        }
    }

    override suspend fun sendMessage(text: String, id: Long) {
        queue.send(text to id)
    }
}