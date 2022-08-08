package ru.homeless.database

import mu.KLogger
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.transactions.transaction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import ru.homeless.messageBundle

enum class Roles {
    BOSS {
        override fun hasMessagePermission() = true

        override fun hasRoleSpreadPermission() = true
        override fun permissionDescription() = messageBundle.getString("boss.permission")

    },
    CURATOR {
        override fun hasMessagePermission() = true

        override fun hasRoleSpreadPermission() = false
        override fun permissionDescription() =
            messageBundle.getString("curator.permission")
    },

    CANDIDATE {
        override fun hasMessagePermission() = false

        override fun hasRoleSpreadPermission() = false
        override fun permissionDescription() = messageBundle.getString("candidate.permission")
    };

    abstract fun hasMessagePermission(): Boolean
    abstract fun hasRoleSpreadPermission(): Boolean
    abstract fun permissionDescription(): String
}

enum class CuratorState {
    START,
    ASK_ROLE,
    REQUEST_MESSAGE,
    SEND_MESSAGE,
    SEND_PHONE_OR_EMAIL_OR_STATUS,
    SCHEDULE_MESSAGE,
    ASK_CONFIRMATION,
    WAITING,
    GRANT_ROLE,
    REQUEST_ON_UPDATE,
    REQUEST_REMOVE_UPDATE,
}

object Curators : LongIdTable() {
    val firstName = varchar("firstName", 200)
    val secondName = varchar("secondName", 200).nullable()
    val state = enumeration("state", CuratorState::class)
    val role = enumeration("role", Roles::class)
    val phone = varchar("phone", 30).nullable()
    val message = reference("message", Messages).nullable()
}

class Curator(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Curator>(Curators)

    var firstName by Curators.firstName
    var secondName by Curators.secondName
    var state by Curators.state
    var role by Curators.role
    var phone by Curators.phone
    var message by Message optionalReferencedOn Curators.message

    fun updateState(newState: CuratorState) = transaction {
        state = newState
    }

    fun updatePhone(phone: Phone, newState: CuratorState = CuratorState.ASK_ROLE, newRole: Roles = Roles.CANDIDATE) = transaction {
        this@Curator.phone = phone.normalizedNumber
        state = newState
        role = newRole
    }

    fun updateMessageAndState(text: String, newState: CuratorState) {
        val message = transaction {
            Message.new {
                curator = this@Curator
                this.text = text
            }
        }
        transaction {
            this@Curator.message = message
            state = newState
        }
    }

    fun lastMessage() = transaction {
        message
    }

    fun updateRole(newRole: Roles, newState: CuratorState = CuratorState.WAITING) = transaction {
        role = newRole
        state = newState
    }
}

fun curatorById(id: Long) = transaction {
    Curator.findById(id)
}

fun updateCuratorStateById(id: Long, newState: CuratorState) = transaction {
    Curator.findById(id)?.state = newState
}

fun insertCurator(
    id: Long,
    firstName: String,
    secondName: String?,
    state: CuratorState = CuratorState.START,
    role: Roles = Roles.CANDIDATE
) = transaction {
    Curator.new(id) {
        this.firstName = firstName
        this.secondName = secondName
        this.state = state
        this.role = role
    }
}

fun updateCuratorRoleById(id: Long, role: Roles) = transaction {
    Curator.findById(id)?.role = role
}

fun checkPermission(
    curator: Curator?,
    absSender: AbsSender,
    chatId: Long,
    permission: Roles.() -> Boolean,
    logger: KLogger
): Boolean {
    if (curator == null) {
        val answer = SendMessage()
        answer.setChatId(chatId)
        logger.error { "Could not find curator with id $chatId" }
        answer.text = messageBundle.getString("unknown.curator")
        try {
            absSender.execute(answer)
        } catch (e: TelegramApiException) {
            logger.error { "Exception while send 'unknown curator' message because of: ${e.message}" }
        }
        return false
    }

    if (!curator.role.permission()) {
        val answer = SendMessage()
        answer.setChatId(curator.id.value)
        logger.warn { "User ${curator.firstName} ${curator.secondName} with role ${curator.role} have not permission on action" }
        answer.text = messageBundle.getString("you.have.not.permission.on")
        try {
            absSender.execute(answer)
        } catch (e: TelegramApiException) {
            logger.error { "Exception while send 'no permission' message because of: ${e.message}" }
        }
        return false
    }
    return true
}


fun checkPermission(
    id: Long,
    absSender: AbsSender,
    permission: Roles.() -> Boolean,
    logger: KLogger
): Boolean {
    return checkPermission(curatorById(id), absSender, id, permission, logger)
}

fun findCuratorByPhone(phone: Phone) = transaction {
    Curator.find { Curators.phone eq phone.normalizedNumber }.firstOrNull()
}