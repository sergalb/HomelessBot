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
import ru.homeless.database.MessagesOnStatusUpdate
import ru.homeless.database.MessagesVolunteers
import ru.homeless.database.SpreadSheetUpdates
import ru.homeless.database.Volunteers
import ru.homeless.google.initSpreadsheetUpdatesDaemon
import ru.homeless.volunteers.volunteersBot
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.inputStream
import kotlin.io.path.reader

val messageBundle: Properties = Properties().also {
    it.load(
        Path.of("/data")
            .resolve("resources")
            .resolve("messages.properties")
            .inputStream()
    )
}

val credentialsPath = Path.of("/data", "credentials")

fun initDb() {
    Database.connect(
        "jdbc:mysql://" +
                getLocalProperty("mysql_host") +
                ":" +
                getLocalProperty("mysql_port") +
                "/" +
                getLocalProperty("mysql_db"),
        "com.mysql.cj.jdbc.Driver",
        user = getLocalProperty("mysql_user"),
        password = getLocalProperty("mysql_password")
    )
}

fun getLocalProperty(key: String): String {
    val properties = Properties()
    val localProperties = credentialsPath.resolve("local.properties")
    localProperties.reader().use {
        properties.load(it)
    }

    return properties.getProperty(key)
}

fun main() {
    initDb()
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Volunteers)
        SchemaUtils.create(Curators)
        SchemaUtils.create(Messages)
        SchemaUtils.create(MessagesVolunteers)
        SchemaUtils.create(SpreadSheetUpdates)
        SchemaUtils.create(MessagesOnStatusUpdate)
    }

    initSpreadsheetUpdatesDaemon()
    try {
        val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
        telegramBotsApi.registerBot(volunteersBot)
        telegramBotsApi.registerBot(curatorsBot)
    } catch (e: TelegramApiException) {
        println("exception ${e.message}")
    }

}