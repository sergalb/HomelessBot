package ru.homeless.google

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import mu.KotlinLogging
import ru.homeless.database.Phone
import ru.homeless.database.Volunteers.status
import ru.homeless.getLocalProperty
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

private val logger = KotlinLogging.logger {}
private const val APPLICATION_NAME = "Google Sheets API Java Quickstart"
private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()
const val TOKENS_DIRECTORY_PATH = "tokens"
private val SPREADSHEET_SCOPES = listOf(SheetsScopes.SPREADSHEETS_READONLY)
private const val CREDENTIALS_FILE_PATH = "google-credentials.json"
private val HTTP_TRANSPORT by lazy { GoogleNetHttpTransport.newTrustedTransport() }
private val service by lazy {
    Sheets.Builder(
        HTTP_TRANSPORT,
        JSON_FACTORY,
        getCredentials(HTTP_TRANSPORT, SPREADSHEET_SCOPES)
    )
        .setApplicationName(APPLICATION_NAME)
        .build()
}

/**
 * Creates an authorized Credential object.
 * @param HTTP_TRANSPORT The network HTTP Transport.
 * @return An authorized Credential object.
 * @throws IOException If the credentials.json file cannot be found.
 */

fun getCredentials(HTTP_TRANSPORT: NetHttpTransport, scopes: List<String>): Credential {
    val credentialsStream = loadResource(CREDENTIALS_FILE_PATH)
        ?: throw FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH)
    val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(credentialsStream))

    // Build flow and trigger user authorization request.
    val flow = GoogleAuthorizationCodeFlow.Builder(
        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes
    )
        .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
        .setAccessType("offline")
        .build()
    val receiver = LocalServerReceiver.Builder().setPort(8888).build()
    return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
}

fun getWholeTable(): List<List<Any>> {
    val range = getLocalProperty("spreadsheet_range")
    val spreadsheetId = getLocalProperty("spreadsheet_id")
    val response = service.spreadsheets().values()[spreadsheetId, range]
        .execute()
    return response.getValues()
}

data class SpreadSheetVolunteer(
    val name: String?,
    val lastName: String?,
    val email: String?,
    val phone: Phone?,
    val status: String?
) {
    companion object {
        fun fromRow(row: List<Any>): SpreadSheetVolunteer? {
            val name = row.getOrNull(3) as? String
            val lastName = row.getOrNull(4) as? String
            val email = row.getOrNull(5) as? String
            val phone = (row.getOrNull(6) as? String)?.let { Phone.byNumber(it) }
            val status = row.getOrNull(16) as? String
            if (phone == null && email == null) return null
            return SpreadSheetVolunteer(name, lastName, email, phone, status)
        }
    }
}


fun findUserByPhone(phone: Phone): SpreadSheetVolunteer? {
    try {
        val table = getWholeTable()

        for (row in table) {
            val phoneCell = row[6]
            val rowPhone = (phoneCell as? String)?.let { Phone.byNumber(it) }
            if (rowPhone == phone) {
                return SpreadSheetVolunteer.fromRow(row)
            }
        }
    } catch (e: FileNotFoundException) {
        logger.error { e.message}
    }
    return null
}

fun loadResource(path: String): InputStream? {
    return Thread.currentThread().contextClassLoader.getResourceAsStream(path)
}
