package ru.homeless.volunteers

import java.time.LocalDateTime

interface MessageQueue {
    suspend fun sendMessage(text: String, id: Long, onError: (Exception) -> Unit = {})
    suspend fun scheduleMessage(text: String, id: Long, date: LocalDateTime, onError: (Exception) -> Unit = {})
}