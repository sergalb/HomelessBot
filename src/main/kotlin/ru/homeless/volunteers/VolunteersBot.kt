package ru.homeless.volunteers

import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import ru.homeless.database.State
import ru.homeless.database.Volunteers
import ru.homeless.database.updatePhone
import ru.homeless.database.volunteersIdByPhones
import ru.homeless.database.volunteersStateById
import ru.homeless.messageBundle
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Properties

val volunteersBot = VolunteersBoot

object VolunteersBoot : TelegramLongPollingCommandBot(), MessageReceiver {
    private val logger = KotlinLogging.logger {}
    private val messageQueue: MessageQueue = MessageQueueImpl(this)

    init {
        register(StartCommand())
        registerDefaultAction { absSender, message ->
            //todo help message
            logger.info {
                """Unknown command used: 
                    user: ${message.chatId},
                    text: ${message.text}""".trimIndent()
            }
        }
    }

    private val volunteersBotToken: String by lazy {
        val properties = Properties()
        val localProperties = File("local.properties")
        if (localProperties.isFile) {
            InputStreamReader(FileInputStream(localProperties), Charsets.UTF_8).use { reader ->
                properties.load(reader)
            }
        } else error("File from not found")

        properties.getProperty("volunteers_bot_token")
    }


    override fun getBotToken(): String = volunteersBotToken

    override fun getBotUsername() = "homeless_volunteers"

    override fun processNonCommandUpdate(update: Update?) {
        val message = update?.message ?: return
        val state = volunteersStateById(message.chatId)
        when (state) {
            State.STARTED -> {
                val normalizedPhone = normalizedPhoneByNumber(
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
                        execute(invalidPhoneMessage)
                    } catch (e: TelegramApiException) {
                        logger.error {
                            "Error while invalid phone message send:  ${
                                e.printStackTrace(
                                    PrintWriter(
                                        StringWriter()
                                    )
                                )
                            }"
                        }
                    }
                    return
                }
                updatePhone(normalizedPhone, State.IDENTIFIED)
                val gratefulMessage = SendMessage()
                gratefulMessage.text = messageBundle.getString("thank.you.for.phone")
                gratefulMessage.setChatId(message.chatId)
                try {
                    execute(gratefulMessage)
                } catch (e: TelegramApiException) {
                    logger.error { "Error while grateful message send: ${e.printStackTrace(PrintWriter(StringWriter()))} " }
                }

            }

        }
    }

    override suspend fun receive(text: String, phones: List<Phone>) {
        val volunteers = volunteersIdByPhones(phones)
        if (volunteers.size != phones.size) {
            logger.error { "Given phones size unequal found volunteers id: got ${phones.size} phones, found ${volunteers.size} volunteers" }
        }

        volunteers.forEach { messageQueue.sendMessage(text, it) }
    }
}

fun normalizedPhoneByNumber(number: String): Phone? {
    val normalized = number.filter { it.isDigit() }
    return if (normalized.all { it.isDigit() } && 11 <= normalized.length && normalized.length <= 15) {
        Phone(normalized)
    } else {
        null
    }
}

data class Phone internal constructor(val normalizedNumber: String)

val database by lazy {
    //todo change path
    Database.connect("jdbc:sqlite:/Users/Sergey.Balakhin/Desktop/homeless/data/notificationBot.db", "org.sqlite.JDBC")
}


fun main() {
    database
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Volunteers)
    }

    try {
        val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
        telegramBotsApi.registerBot(volunteersBot)
    } catch (e: TelegramApiException) {
        println("exception ${e.message}")
    }

}
