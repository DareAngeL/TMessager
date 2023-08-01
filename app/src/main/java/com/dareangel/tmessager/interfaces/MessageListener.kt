package com.dareangel.tmessager.interfaces

import com.dareangel.tmessager.model.MessageData

interface MessageListener {

    fun onFetchMessages(messages: ArrayList<MessageData>)
    fun onFetchMessagesFromLoadMore(messages: ArrayList<MessageData>? = null)
    fun onMessageSent(msg: MessageData)
    fun onMessageSeen(msg: MessageData)
    fun onMessageFailed()
    fun onNewMessage(message: MessageData)
    fun onMessageSending(msg: MessageData)
}