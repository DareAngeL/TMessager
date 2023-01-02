package com.dareangel.tmessager.data.model.interfaces

interface IServerListener {
    /**
     * Send the message to the other peer
     * @param it the data
     */
    fun sendMessage(it: HashMap<String, Any>)
    /**
     * Closes the application
     */
    fun closeApplication()

    /**
     * When the app connected to the server
     */
    fun onConnect(isFirstInit: Boolean)

    /**
     * When the app disconnects from the server
     */
    fun onDisconnect(it: Array<Any>)

    /**
     * When the app can't connect to the server
     */
    fun onConnectError()

    /**
     * When there is new message from the other peer
     */
    fun onNewMessage(it: Array<Any>)

    /**
     * Seen the message that was saved from the client side
     */
    fun seenMessage()

    /**
     * When the message of the client was successfully sent
     */
    fun onMessageSent(it: Array<Any>)

    /**
     * When the message of the client was successfully seen
     */
    fun onMessageReceived(it: Array<Any>)

    /**
     * When the other peer joined the chat room
     */
    fun onUserJoined(it: Array<Any>)

    /**
     * When the client joined the chat room
     */
    fun onEnteredRoom(it: Array<Any>)

    fun onTyping(it: Array<Any>)
    fun onStopTyping(it: Array<Any>)
}