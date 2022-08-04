package ru.homeless

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
import ru.homeless.database.Phone
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

fun loadResource(path: String): InputStream? {
    return Thread.currentThread().contextClassLoader.getResourceAsStream(path)
}


const val APPLICATION_NAME = "Google Sheets API Java Quickstart"
val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()
const val TOKENS_DIRECTORY_PATH = "tokens"

/**
 * Global instance of the scopes required by this quickstart.
 * If modifying these scopes, delete your previously saved tokens/ folder.
 */
private val SCOPES = listOf(SheetsScopes.SPREADSHEETS_READONLY)
private const val CREDENTIALS_FILE_PATH = "/google-credentials.json"

/**
 * Creates an authorized Credential object.
 * @param HTTP_TRANSPORT The network HTTP Transport.
 * @return An authorized Credential object.
 * @throws IOException If the credentials.json file cannot be found.
 */

fun getCredentials(HTTP_TRANSPORT: NetHttpTransport): Credential {
    // Load client secrets.
    val credentialsStream = loadResource(CREDENTIALS_FILE_PATH)
        ?: throw FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH)
    val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(credentialsStream))

    // Build flow and trigger user authorization request.
    val flow = GoogleAuthorizationCodeFlow.Builder(
        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
    )
        .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
        .setAccessType("offline")
        .build()
    val receiver = LocalServerReceiver.Builder().setPort(8888).build()
    return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
}


/**
 * Prints the names and majors of students in a sample spreadsheet:
 * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
 */
fun main() {
    // Build a new authorized API client service.
    val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
    val range = "Волонтеры Ночлежки!A2:H"
    val spreadsheetId = "1sROeG2qyySNtC_bXzwqpfEwBQyagFVEZUpsL3zSjfUU"
    val service = Sheets.Builder(
        HTTP_TRANSPORT,
        JSON_FACTORY,
        getCredentials(HTTP_TRANSPORT)
    )
        .setApplicationName(APPLICATION_NAME)
        .build()
    val response = service.spreadsheets().values()[spreadsheetId, range]
        .execute()
    val values = response.getValues()
    if (values == null || values.isEmpty()) {
        println("No data found.")
        return
    }

}

data class SpreadSheetVolunteer(
    val name: String,
    val lastName: String,
    val email: String,
    val phone: Phone,
    val status: String
)


fun findUserByPhone(phone: Phone): SpreadSheetVolunteer? {
    //todo not implemented
    return null
}
