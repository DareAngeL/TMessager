package com.dareangel.tmessager.data.model.interfaces

import android.content.Context
import com.dareangel.tmessager.data.model.Message

interface IMessagesTable {

    fun interface OnFetchingListener {
        fun onFinishedFetching(messages: ArrayList<Message>)
    }

    /**
     * Opens messages database
     * @param cn the application's context
     */
    fun open(cn: Context) : IMessagesTable

    /**
     * Closes the messages database
     * @param alsoCloseCoroutine also closes the coroutine
     */
    fun close(alsoCloseCoroutine: Boolean)

    /**
     * Add new message to the database
     * @param msg the message to add
     */
    fun addMessage(msg: Message)

    /**
     * Fetches the messages from the database
     * @param listener a listener to know if fetching is done or not.
     */
    fun fetchMessages(listener: OnFetchingListener)

    /**
     * Updates the status of the message
     * @param msg a new message to update
     */
    fun updateMessageStatus(msg: Message)
}