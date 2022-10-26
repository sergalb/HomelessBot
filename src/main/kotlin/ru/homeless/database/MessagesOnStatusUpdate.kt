package ru.homeless.database

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

object MessagesOnStatusUpdate : Table() {
    val oldStatus = varchar("oldStatus", 200)
    val newStatus = varchar("newStatus", 200)
    val message = text("message")
    override val primaryKey = PrimaryKey(oldStatus, newStatus)
}

data class MessageOnStatusUpdate(
    val oldStatus: String,
    val newStatus: String,
    val message: String
) {
    override fun toString(): String {
        return """
            $oldStatus
            $newStatus
            $message
               """.trimIndent()
    }
}

fun insertOrUpdateMessage(oldStatus: String, newStatus: String, message: String) = transaction {
    MessagesOnStatusUpdate.insertOnDuplicateKeyUpdate(
        listOf(
            MessagesOnStatusUpdate.oldStatus, MessagesOnStatusUpdate.newStatus, MessagesOnStatusUpdate.message
        )
    ) {
        it[MessagesOnStatusUpdate.oldStatus] = oldStatus
        it[MessagesOnStatusUpdate.newStatus] = newStatus
        it[MessagesOnStatusUpdate.message] = message
    }
}

fun findMessageByStatuses(oldStatus: String, newStatus: String) = transaction {
    MessagesOnStatusUpdate.select {
        (MessagesOnStatusUpdate.oldStatus eq oldStatus) and (MessagesOnStatusUpdate.newStatus eq newStatus)
    }.firstOrNull()?.get(MessagesOnStatusUpdate.message)
}


fun deleteMessagesWithStatuses(oldStatus: String, newStatus: String) = transaction {
    MessagesOnStatusUpdate.deleteWhere { (MessagesOnStatusUpdate.oldStatus eq oldStatus) and (MessagesOnStatusUpdate.newStatus eq newStatus) }
}

fun selectAllMessages() = transaction {
    MessagesOnStatusUpdate.selectAll().map {
        MessageOnStatusUpdate(
            it[MessagesOnStatusUpdate.oldStatus],
            it[MessagesOnStatusUpdate.newStatus],
            it[MessagesOnStatusUpdate.message],
        )
    }
}

class InsertUpdateOnDuplicate(table: Table, val onDupUpdate: List<Column<*>>) : InsertStatement<Number>(table) {
    override fun prepareSQL(transaction: Transaction): String {
        val onUpdateSQL = if (onDupUpdate.isNotEmpty()) {
            " ON DUPLICATE KEY UPDATE " + onDupUpdate.joinToString {
                "${transaction.identity(it)}=VALUES(${
                    transaction.identity(
                        it
                    )
                })"
            }
        } else ""
        return super.prepareSQL(transaction) + onUpdateSQL
    }
}

fun <T : Table> T.insertOnDuplicateKeyUpdate(
    onDupUpdateColumns: List<Column<*>>, body: T.(InsertUpdateOnDuplicate) -> Unit
) {
    val insert = InsertUpdateOnDuplicate(this, onDupUpdateColumns)
    body(insert)
    TransactionManager.current().exec(insert)

}