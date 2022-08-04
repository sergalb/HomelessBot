package ru.homeless.database

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.transactions.transaction
import ru.homeless.SpreadSheetVolunteer

enum class VolunteerState {
    STARTED, IDENTIFIED
}

object Volunteers : LongIdTable() {
    val firstName = text("firstName")
    val secondName = text("secondName").nullable()
    val state = enumeration("state", VolunteerState::class)
    val phone = text("phone").nullable().uniqueIndex()
    val email = text("email").nullable().uniqueIndex()
    val status = text("status").nullable()

}

class Volunteer(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Volunteer>(Volunteers)

    var firstName by Volunteers.firstName
    var secondName by Volunteers.secondName
    var state by Volunteers.state
    var phone by Volunteers.phone
    var email by Volunteers.email
    var status by Volunteers.status
    val messages by Message via MessagesVolunteers
}

fun volunteersStateById(id: Long) = transaction {
    Volunteer.findById(id)?.state
}

fun updatePhoneAndState(id: Long, phone: Phone, newState: VolunteerState) = transaction {
    val volunteer = Volunteer.findById(id)
    volunteer?.phone = phone.normalizedNumber
    volunteer?.state = newState

}

fun updateVolunteer(id: Long, spreadSheetVolunteer: SpreadSheetVolunteer, newState: VolunteerState) = transaction {
    val volunteer = Volunteer.findById(id)
    volunteer?.firstName = spreadSheetVolunteer.name
    volunteer?.secondName = spreadSheetVolunteer.lastName
    volunteer?.phone = spreadSheetVolunteer.phone.normalizedNumber
    volunteer?.email = spreadSheetVolunteer.email
    volunteer?.status = spreadSheetVolunteer.status
    volunteer?.state = newState
}

fun volunteersIdByPhones(phones: List<Phone>) = transaction {
    Volunteer.find { Volunteers.phone inList phones.map { it.normalizedNumber } }
        .map { it.id.value }
}

fun insertVolunteer(
    id: Long,
    firstName: String,
    secondName: String?,
    state: VolunteerState,
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

fun findVolunteerByPhone(phone: Phone) = transaction {
    Volunteer.find {
        Volunteers.phone eq phone.normalizedNumber
    }.firstOrNull()
}