package com.dareangel.tmessager.data.model.interfaces

fun interface UnseenMessagesListener {

    fun onNewUnseenMessage(count: Int)
}