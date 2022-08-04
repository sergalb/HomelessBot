package ru.homeless.database

fun<T> personByContact(contact: String, byPhone: (Phone) -> T): T? {
    val contactAsPhone = Phone.normalizedPhoneByNumber(contact)
    if (contactAsPhone != null) {
        return byPhone(contactAsPhone)
    } else {
        throw RuntimeException("")
    }
}