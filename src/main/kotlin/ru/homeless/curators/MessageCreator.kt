package ru.homeless.curators

import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.bots.AbsSender
import ru.homeless.database.Curator

interface MessageCreator {
    fun onText(curator: Curator, message: Message, absSender: AbsSender)
    fun onVolunteers(curator: Curator, message: Message, absSender: AbsSender)
    fun onSchedule(curator: Curator, message: Message, absSender: AbsSender)
    fun onConfirmation(curator: Curator, message: Message, absSender: AbsSender)
}