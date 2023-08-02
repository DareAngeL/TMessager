package com.dareangel.tmessager.service.comm

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.dareangel.tmessager.`object`.MessengerCodes
import com.dareangel.tmessager.service.MessagingService

/**
 * Handler of incoming messages from UI@ChatRoomFragment to MessagingService.
 */
class IncomingDataFromUIHandler(
    private val msgService: MessagingService
) : Handler(Looper.getMainLooper())
{

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            // send message to other user
            MessengerCodes.SEND_MSG -> {
                val data = msg.data
                val message = data.getString("data")
                msgService.sendMessage(message!!)
            }

            MessengerCodes.FETCH_MSGS -> {
                msgService.fetchMessages()
            }

            MessengerCodes.LOAD_MORE_MSGS -> {
                msgService.loadMoreMessages()
            }

            MessengerCodes.REMOVE_MESSENGER_CLIENT -> {
                msgService.removeMessengerClient()
            }

            MessengerCodes.FETCH_UNSEEN_MSGS -> {
                msgService.fetchUnseenMessages()
            }
        }
    }


}