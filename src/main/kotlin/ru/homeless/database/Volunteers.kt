package ru.homeless.database

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.homeless.google.SpreadSheetVolunteer

enum class VolunteerState {
    STARTED, IDENTIFIED
}

private val EMAIL_PATTERN = Regex(
    "[a-zA-Z0-9+._%\\-]{1,256}" +
            "@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
)

object Volunteers : LongIdTable() {
    val firstName = varchar("firstName", 200)
    val secondName = varchar("secondName", 200).nullable()
    val state = enumeration("state", VolunteerState::class)
    val phone = varchar("phone", 30).nullable().uniqueIndex()
    val email = varchar("email", 200).nullable().uniqueIndex()
    val status = varchar("status", 200).nullable()

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

    fun updatePhoneAndState(phone: Phone, newState: VolunteerState?) = transaction {
        this@Volunteer.phone = phone.normalizedNumber
        newState?.let {
            this@Volunteer.state = newState
        }
    }

    fun updatePhone(phone: String) {
        Phone.byNumber(phone)?.let { updatePhoneAndState(it, null) }
    }

    fun updateName(newName: String) = transaction {
        firstName = newName
    }

    fun updateSecondName(newSecondName: String) = transaction {
        secondName = newSecondName
    }

    fun updateEmail(newEmail: String) = transaction {
        email = newEmail
    }

    fun updateStatus(newStatus: String) = transaction {
        status = newStatus
    }
}

fun getVolunteersCount() = transaction {
    Volunteers.selectAll().count()
}

fun volunteersStateById(id: Long) = transaction {
    Volunteer.findById(id)?.state
}

fun updatePhoneAndState(id: Long, phone: Phone, newState: VolunteerState) = transaction {
    Volunteer.findById(id)?.updatePhoneAndState(phone, newState)
}

fun updateVolunteer(
    id: Long,
    spreadSheetVolunteer: SpreadSheetVolunteer,
    normalizedPhone: Phone,
    newState: VolunteerState
) = transaction {
    val volunteer = Volunteer.findById(id)
    spreadSheetVolunteer.name?.also { volunteer?.firstName = it }
    spreadSheetVolunteer.lastName?.also { volunteer?.secondName = it }
    spreadSheetVolunteer.email.also { volunteer?.email = it }
    spreadSheetVolunteer.status?.also { volunteer?.status = it }
    volunteer?.state = newState

    val newPhone = spreadSheetVolunteer.phone?.normalizedNumber ?: normalizedPhone.normalizedNumber
    volunteer?.phone = newPhone
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

fun findVolunteerByStatus(status: String) = transaction {
    Volunteer.find {
        Volunteers.status eq status
    }.firstOrNull()
}

fun findVolunteerByEmail(email: String?): Volunteer? {
    return email?.let {
        if (email.matches(EMAIL_PATTERN)) {
            transaction {
                Volunteer.find { Volunteers.email eq email }.firstOrNull()
            }
        } else null
    }
}

fun findVolunteerByPhone(phone: String?): Volunteer? =
    phone?.let { Phone.byNumber(phone)?.let { findVolunteerByPhone(it) } }

fun findVolunteerByContact(contact: String): Volunteer? {
    return findVolunteerByPhone(contact) ?: findVolunteerByEmail(contact) ?: findVolunteerByStatus(contact)
}


fun isStatusExistForVolunteers(status: String) = transaction {
    !Volunteers.slice(Volunteers.status).select { Volunteers.status eq status }.empty()
}
