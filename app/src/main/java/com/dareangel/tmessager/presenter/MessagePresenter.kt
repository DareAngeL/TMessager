package com.dareangel.tmessager.presenter

import android.content.Context
import android.os.Messenger
import com.dareangel.tmessager.interfaces.MessageListener
import com.dareangel.tmessager.model.Message
import com.dareangel.tmessager.model.MessageData

class MessagePresenter(
    cn: Context,
    private val msgListener: MessageListener
) : MessageListener
{

    private val message = Message(cn).apply {
        msgListener = this@MessagePresenter
    }

    fun setUIMessenger(messenger: Messenger?) {
        message.uiMessenger = messenger
    }

    fun sendMessage(msg: String) {
        message.send(msg)
    }

    fun fetchMessages() {
        message.fetchMessages()
    }

    fun fetchUnseenMessages() {
        message.fetchUnseenMessages()
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