package ru.homeless.volunteers


interface MessageReceiver {
    suspend fun receive(text: String, phones: List<Phone>)
}