package com.dareangel.tmessager.service.comm

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.dareangel.tmessager.model.MessageData
import com.dareangel.tmessager.`object`.MessengerCodes
import com.dareangel.tmessager.ui.view.fragments.ChatRoomFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Handler of incoming messages from MessagingService to UI@ChatRoomFragment.kt.
 */
@Suppress("UNCHECKED_CAST")
class IncomingDataFromServiceHandler(private val chatRoomFragmentContext: ChatRoomFragment) : Handler(Looper.getMainLooper()) {
    override fun handleMessage(msg: Message) {
        when (msg.what) {

            MessengerCodes.MSG_SENT -> {

                val message = getJSONStringParsedData(msg.data.getString("data")!!, object : TypeToken<MessageData>() {})
                chatRoomFragmentContext.onMessageSent(message as MessageData)
            }

            MessengerCodes.MSG_FAILED -> {
                chatRoomFragmentContext.onMessageFailed()
            }

            MessengerCodes.FETCH_MSGS -> {

                val msgList = getJSONStringParsedData(msg.data.getString("data")!!, object : TypeToken<ArrayList<MessageData>>() {})
                chatRoomFragmentContext.onFetchMessages(msgList as ArrayList<MessageData>)
            }

            MessengerCodes.MSG_SENDING -> {

                val message = getJSONStringParsedData(msg.data.getString("data")!!, object : TypeToken<MessageData>() {})
                chatRoomFragmentContext.onMessageSending(message as MessageData)
            }

            MessengerCodes.LOAD_MORE_MSGS -> {

                if (msg.data.getString("data") == null) {
                    chatRoomFragmentContext.onFetchMessagesFromLoadMore()
                    return
                }

                val msgList = getJSONStringParsedData(msg.data.getString("data")!!, object : TypeToken<ArrayList<MessageData>>() {})
                chatRoomFragmentContext.onFetchMessagesFromLoadMore(msgList as ArrayList<MessageData>)
            }

            MessengerCodes.NEW_MSG -> {
                val message = getJSONStringParsedData(msg.data.getString("data")!!, object : TypeToken<MessageData>() {})
                chatRoomFragmentContext.onNewMessage(message as MessageData)
            }

            MessengerCodes.MSG_SEEN -> {
                val message = getJSONStringParsedData(msg.data.getString("data")!!, object : TypeToken<MessageData>() {})
                chatRoomFragmentContext.onMessageSeen(message as MessageData)
            }
        }
    }

    private fun getJSONStringParsedData(data: String, typeToken: TypeToken<*>): Any {
        val type: Type = typeToken.type
        return Gson().fromJson(data, type)
    }
}