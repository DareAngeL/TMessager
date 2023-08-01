package com.dareangel.tmessager.presenter

import com.dareangel.tmessager.interfaces.MessageListener
import com.dareangel.tmessager.model.Message
import com.dareangel.tmessager.model.MessageData

class MessagePresenter(
    private val msgListener: MessageListener
) : MessageListener
{

    private val message = Message().apply {
        msgListener = this@MessagePresenter
    }

    fun sendMessage(msg: String) {
        message.send(msg)
    }

    fun fetchMessages() {
        message.fetchMessages()
    }

    fun loadMoreMessages() {
        message.loadMoreMessages()
    }

    override fun onFetchMessages(messages: ArrayList<MessageData>) {
        msgListener.onFetchMessages(messages)
    }

    override fun onFetchMessagesFromLoadMore(messages: ArrayList<MessageData>?) {
        msgListener.onFetchMessagesFromLoadMore(messages)
    }

    override fun onMessageSent(msg: MessageData) {
        msgListener.onMessageSent(msg)
    }

    override fun onMessageSeen(msg: MessageData) {
        msgListener.onMessageSeen(msg)
    }

    override fun onMessageFailed() {
        msgListener.onMessageFailed()
    }

    override fun onNewMessage(message: MessageData) {
        msgListener.onNewMessage(message)
    }

    override fun onMessageSending(msg: MessageData) {
        msgListener.onMessageSending(msg)
    }
}