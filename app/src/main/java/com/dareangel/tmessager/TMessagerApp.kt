package com.dareangel.tmessager

import android.app.Application
import io.socket.client.Socket

class TMessagerApp : Application() {
    private val TAG = "TMessagerApp"

    private lateinit var mSocket : Socket

    /**
     * getSocket()
     * @return the connected socket
     */
    fun socket() : Socket = mSocket
}