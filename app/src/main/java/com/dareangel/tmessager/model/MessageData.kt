package com.dareangel.tmessager.model

data class MessageData(
    var id: String = "",
    val sender: String = "",
    var message: String = "",
    var timestamp: Long = 0L,
    var status: Int = -1,
    var seenBy: String = ""
) {

    companion object {
        const val STATUS_SENT = 0
        const val STATUS_SENDING = 1
        const val STATUS_SEEN = 2 // TODO: implement seen status
    }
}