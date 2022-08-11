package ru.homeless.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime


object Messages : IntIdTable() {
    val curator = reference("curator", Curators, onDelete = ReferenceOption.CASCADE)
    val text = text("text")
    val schedule = datetime("schedule").nullable()
}

object MessagesVolunteers : Table() {
    val message = reference("message", Messages, onDelete = ReferenceOption.CASCADE)
    val volunteer = reference("volunteer", Volunteers, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(message, volunteer)
}

class Message(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Message>(Messages)

    var curator by Curator referencedOn Messages.curator
    var text by Messages.text
    var schedule by Messages.schedule
    var volunteers by Volunteer via MessagesVolunteers

    fun volunteersAsList() = transaction {
        this@Message.volunteers.toList()
    }

    fun addVolunteersToMessage(volunteers: Collection<Volunteer>) = transaction {
        this@Message.volunteers = SizedCollection(volunteers)
    }

    fun addScheduleToMessage(date: LocalDateTime) = transaction {
        schedule = date
    }
}



