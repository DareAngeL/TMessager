package com.dareangel.tmessager.data.model.interfaces

import android.content.Context
import com.dareangel.tmessager.data.model.Message

interface IMessagesTable {

    fun interface OnFetchingListener {
        fun onFinishedFetching(messages: ArrayList<Message>)
    }

    fun open(cn: Context) : IMessagesTable
    fun close(alsoCloseCoroutine: Boolean)
    fun addMessage(msg: Message)
    fun fetchMessages(listener: OnFetchingListener)
    fun updateMessageStatus(msg: Message)
}