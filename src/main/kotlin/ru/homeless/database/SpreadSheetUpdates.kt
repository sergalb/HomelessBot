package ru.homeless.database

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.IllegalArgumentException

enum class Field {
    FIRST_NAME,
    SECOND_NAME,
    PHONE,
    EMAIL,
    STATUS;

    companion object {
        fun fieldByColumnIndex(index: Int): Field {
            return when (index) {
                4 -> FIRST_NAME
                5 -> SECOND_NAME
                7 -> PHONE
                6 -> EMAIL
                18 -> STATUS
                else -> throw IllegalArgumentException("Field can not be '$index'. 4, 5, 6, 7, 18 is allowed")
            }
        }
    }
}

object SpreadSheetUpdates : LongIdTable() {
    val fieldIndex = integer("field")
    val oldValue = varchar("oldValue", 200)
    val newValue = varchar("newValue", 200)
    val volunteerPhone = varchar("phone", 30).nullable()
    val voluteerEmail = varchar("email", 200).nullable()
}

class SpreadSheetUpdate(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<SpreadSheetUpdate>(SpreadSheetUpdates)

    var fieldIndex by SpreadSheetUpdates.fieldIndex
    var oldValue by SpreadSheetUpdates.oldValue
    var newValue by SpreadSheetUpdates.newValue
    var volunteerPhone by SpreadSheetUpdates.volunteerPhone
    var volunteerEmail by SpreadSheetUpdates.voluteerEmail
}

fun takeAllUpdatesAndClean() = transaction {
    val res = SpreadSheetUpdate.all().toList()
    SpreadSheetUpdates.deleteAll()
    res
}