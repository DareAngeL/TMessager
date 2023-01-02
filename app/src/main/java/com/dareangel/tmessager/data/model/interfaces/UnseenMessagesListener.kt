package com.dareangel.tmessager.data.model.interfaces

fun interface UnseenMessagesListener {

    /**
     * When there's new unseen messages
     */
    fun onNewUnseenMessage(count: Int)
}