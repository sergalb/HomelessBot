package ru.homeless.database

class Phone private constructor(val normalizedNumber: String)
{
    override fun equals(other: Any?): Boolean {
        if (other !is Phone) return false
        return other === this || other.normalizedNumber == normalizedNumber
    }

    override fun hashCode(): Int {
        return normalizedNumber.hashCode()
    }

    override fun toString(): String {
        return normalizedNumber
    }

    companion object {
        fun normalizedPhoneByNumber(number: String): Phone? {
            val normalized = number.filter { it.isDigit() }
            return if (normalized.all { it.isDigit() } && 11 <= normalized.length && normalized.length <= 15) {
                Phone(normalized)
            } else {
                null
            }
        }
    }
}