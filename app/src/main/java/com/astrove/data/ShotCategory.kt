package com.astrove.data

/** Auto-category for a screenshot. Stored on the row; assigned by the classifier. */
enum class ShotCategory(val label: String) {
    RECEIPT("Receipts"),
    CHAT("Chats"),
    DOC("Docs"),
    LINK("Links"),
    QR("QR codes"),
    TICKET("Tickets"),
    MAP("Maps"),
    OTHER("Other"),
}
