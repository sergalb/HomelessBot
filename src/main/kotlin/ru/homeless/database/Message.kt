package ru.homeless.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction


object Messages : IntIdTable() {
    val curator = reference("curator", Curators)
    val text = text("text")
    val schedule = datetime("schedule").nullable()
}

object MessagesVolunteers : Table() {
    val message = reference("message", Messages)
    val volunteer = reference("volunteer", Volunteers)
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

    fun addVolunteersToMessage(volunteers: List<Volunteer>) = transaction {
        this@Message.volunteers = SizedCollection(volunteers)
    }
}



