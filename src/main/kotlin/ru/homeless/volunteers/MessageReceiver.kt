package ru.homeless.volunteers


interface MessageReceiver {
    suspend fun receive(text: String, ids: List<Long>)
}