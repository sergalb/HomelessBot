package ru.homeless.database

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.homeless.volunteers.Phone

enum class State {
    UNSTARTED, STARTED, IDENTIFIED
}

object Volunteers : IdTable<Long>() {
    val firstName = text("firstName")
    val secondName = text("secondName")
    val state = enumeration("state", State::class)
    val phone = text("phone").nullable()
    val email = text("email").nullable()
    override val id = long("id").entityId()

}

class Volunteer(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Volunteer>(Volunteers)

    var firstName by Volunteers.firstName
    var secondName by Volunteers.secondName
    var state by Volunteers.state
    var phone by Volunteers.phone
    var email by Volunteers.email
}

fun volunteersStateById(id: Long) = transaction {
    Volunteers.slice(Volunteers.state)
        .select(Volunteers.id eq id)
        .map { it[Volunteers.state] }
        .firstOrNull()
}

fun updatePhone(phone: Phone, newState: State) = transaction {
    Volunteers.update {
        it[Volunteers.phone] = phone.normalizedNumber
        it[state] = newState
    }
}

fun volunteersIdByPhones(phones: List<Phone>) = transaction {
    Volunteers.slice(Volunteers.id)
        .select(Volunteers.phone inList phones.map { it.normalizedNumber })
        .map { it[Volunteers.id].value }

}

fun insertVolunteer(
    id: Long,
    firstName: String,
    secondName: String,
    state: State,
    phone: String? = null,
    email: String? = null
) = transaction {
    Volunteer.new(id) {
        this.firstName = firstName
        this.secondName = secondName
        this.state = state
        this.phone = phone
        this.email = email
    }
}