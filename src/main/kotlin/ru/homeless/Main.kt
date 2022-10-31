package ru.homeless

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import ru.homeless.curators.curatorsBot
import ru.homeless.database.*
import ru.homeless.google.initSpreadsheetUpdatesDaemon
import ru.homeless.volunteers.volunteersBot
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.inputStream
import kotlin.io.path.reader
import kotlin.time.Duration.Companion.minutes

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


@OptIn(DelicateCoroutinesApi::class)
fun main() {
    val logger = KotlinLogging.logger {}
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
    GlobalScope.launch {
        while (true) {
            val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
            try {
                val volunteersSession = telegramBotsApi.registerBot(volunteersBot)
                val curatorsSession = telegramBotsApi.registerBot(curatorsBot)
                while (true) {
                    if (!volunteersSession.isRunning) {
                        volunteersSession.start()
                    }
                    if (!curatorsSession.isRunning) {
                        curatorsSession.start()
                    }
                    delay((5).minutes)
                }
            } catch (e: Exception) {
                logger.error(e) {}
            }
        }
    }

}