package com.dareangel.tmessager.data.model

import java.util.UUID

/**
 * The model for the message
 * @param sender who is the sender of the message
 * @param msg the message to send
 * @param pos the index position of this message on its data group
 * @param status the status of the message, either sending/sent or not sent
 */
data class Message(
    val id: String? = null,
    val msg: String? = null,
    val sender: String? = null,
    val pos: Int? = null,
    val status: String? = null
) {
    companion object {
        // This is to know if the message is from the user or from its chat mate
        // # region: MessageType
        val USER = 0
        val CHAT_MATE = 1
        // # region end
        // # region: send status
        val SENT = "Sent"
        val NOT_SENT = "Not sent"
        val SENDING = "Sending"
        val SEEN = "Seen"
        // # region end
        val LAST_MSG = 0
        val SECOND_LAST_MSG = 1
        val MSG_INSERTED = 3

        enum class Type {
            RESEND, SEND
        }

        fun getUniqueID() : String = UUID.randomUUID().toString()
    }
}