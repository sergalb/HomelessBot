package ru.homeless.google


import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.homeless.database.Field
import ru.homeless.database.findMessageByStatuses
import ru.homeless.database.findVolunteerByPhone
import ru.homeless.database.takeAllUpdatesAndClean
import ru.homeless.volunteers.volunteersBot
import kotlin.time.Duration.Companion.minutes

private val DELAY = (1).minutes

@OptIn(DelicateCoroutinesApi::class)
fun initSpreadsheetUpdatesDaemon() = GlobalScope.launch {
    while (true) {
        val updates = takeAllUpdatesAndClean()
        if (updates.isNotEmpty()) {
            for (update in updates) {
                //todo add search by email
                val volunteer = findVolunteerByPhone(update.volunteerPhone)
                if (volunteer != null) {
                    when (update.field) {
                        Field.FIRST_NAME -> volunteer.updateName(update.newValue)
                        Field.SECOND_NAME -> volunteer.updateSecondName(update.newValue)
                        Field.PHONE -> volunteer.updatePhone(update.newValue)
                        Field.EMAIL -> volunteer.updateEmail(update.newValue)
                        Field.STATUS -> {
                            val message = findMessageByStatuses(update.oldValue, update.newValue)
                            if (message != null) {
                                volunteersBot.receive(message, listOf(volunteer.id.value))
                            }
                            volunteer.updateStatus(update.newValue)
                        }
                    }
                }
            }
        }
        delay(DELAY)
    }
}
