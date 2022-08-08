package ru.homeless

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

fun keyboardWithContact(): ReplyKeyboardMarkup {
    val keyboard = KeyboardRow(
        listOf(
            KeyboardButton(messageBundle.getString("get.phone.keyboard"), true, false, null, null)
        )
    )
    return ReplyKeyboardMarkup.builder()
        .oneTimeKeyboard(true)
        .resizeKeyboard(true)
        .keyboardRow(keyboard)
        .build()
}

fun AbsSender.sendMessage(text: String, chatId: Long, onException: (TelegramApiException) -> Unit = {}) {
    val answer = SendMessage()
    answer.setChatId(chatId)
    answer.text = text
    try {
        execute(answer)
    } catch (e: TelegramApiException) {
        onException(e)
    }
}