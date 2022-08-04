package ru.homeless

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import ru.homeless.curators.curatorsBot
import ru.homeless.database.Curators
import ru.homeless.database.Messages
import ru.homeless.database.MessagesVolunteers
import ru.homeless.database.Volunteers
import ru.homeless.volunteers.volunteersBot
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.Properties
import java.util.ResourceBundle

val messageBundle = ResourceBundle.getBundle("messages")

val database by lazy {
    //todo change path
    Database.connect("jdbc:sqlite:/Users/Sergey.Balakhin/Desktop/homeless/data/notificationBot.db", "org.sqlite.JDBC")
}
fun getBotToken(key: String): String {
    val properties = Properties()
    val localProperties = File("local.properties")
    if (localProperties.isFile) {
        InputStreamReader(FileInputStream(localProperties), Charsets.UTF_8).use { reader ->
            properties.load(reader)
        }
    } else error("File from not found")

    return properties.getProperty(key)
}
fun main() {
    database
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Volunteers)
        SchemaUtils.create(Curators)
        SchemaUtils.create(Messages)
        SchemaUtils.create(MessagesVolunteers)
    }

    try {
        val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
        telegramBotsApi.registerBot(volunteersBot)
        telegramBotsApi.registerBot(curatorsBot)
    } catch (e: TelegramApiException) {
        println("exception ${e.message}")
    }

}