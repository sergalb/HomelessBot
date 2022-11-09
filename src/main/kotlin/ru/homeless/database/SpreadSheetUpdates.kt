package ru.homeless.database

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction

enum class Field(columnIndex: Int) {
    FIRST_NAME(4),
    SECOND_NAME(5),
    PHONE(7),
    EMAIL(6),
    STATUS(18)

}

object SpreadSheetUpdates: LongIdTable() {
    val field = enumeration("field", Field::class)
    val oldValue = varchar("oldValue", 200)
    val newValue = varchar("newValue", 200)
    val volunteerPhone = varchar("phone", 30).nullable()
    val voluteerEmail = varchar("email", 200).nullable()
}

class SpreadSheetUpdate(id: EntityID<Long>): LongEntity(id) {
    companion object : LongEntityClass<SpreadSheetUpdate>(SpreadSheetUpdates)
    var field by SpreadSheetUpdates.field
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