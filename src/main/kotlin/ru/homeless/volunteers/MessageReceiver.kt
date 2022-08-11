package ru.homeless.volunteers

import java.time.LocalDateTime


interface MessageReceiver {
    suspend fun receive(text: String, ids: List<Long>, date: LocalDateTime, onError: (Int, Exception) -> Unit = { _, _ -> })
    suspend fun receive(text: String, ids: List<Long>, onError: (Int, Exception) -> Unit = { _, _ -> })
}