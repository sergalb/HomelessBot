package ru.homeless.database

fun<T> personByContact(contact: String, byPhone: (Phone) -> T): T? {
    val contactAsPhone = Phone.byNumber(contact)
    if (contactAsPhone != null) {
        return byPhone(contactAsPhone)
    } else {
        throw RuntimeException("")
    }
}