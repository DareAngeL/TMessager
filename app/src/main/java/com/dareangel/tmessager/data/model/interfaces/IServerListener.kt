package com.dareangel.tmessager.data.model.interfaces

import org.json.JSONObject

interface IServerListener {
    fun sendMessage(it: HashMap<String, Any>)
    fun closeApplication()
    fun onConnect()
    fun onDisconnect(it: Array<Any>)
    fun onConnectError()
    fun onNewMessage(it: Array<Any>)
    fun seenMessage()
    fun onMessageSent(it: Array<Any>)
    fun onMessageReceived(it: Array<Any>)
    fun onUserJoined(it: Array<Any>)
    fun onEnteredRoom(it: Array<Any>)
    fun onTyping(it: Array<Any>)
    fun onStopTyping(it: Array<Any>)
}