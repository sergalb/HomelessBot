package ru.homeless

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

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