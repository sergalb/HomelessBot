package ru.homeless.volunteers

interface MessageQueue {
    suspend fun sendMessage(text: String, id: Long)
}