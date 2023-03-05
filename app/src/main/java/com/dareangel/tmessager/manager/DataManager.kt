package com.dareangel.tmessager.manager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import com.dareangel.tmessager.data.database.MessagesDB
import com.dareangel.tmessager.data.database.SocketService
import com.dareangel.tmessager.data.model.interfaces.IServerListener
import com.dareangel.tmessager.ui.view.ChatHandler
import com.dareangel.tmessager.util.Utility
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * This is the presenter
 * @param invoker the class name of the invoker of this class
 */
@Suppress("FunctionName", "LocalVariableName")
class DataManager(private val invoker: String) {
    private var mSocket : TSocket? = null

    private val mMessagesDB = MessagesDB()

    // # region: access properties
    val socket: TSocket?
        get() = mSocket

    val msgsDBTable: MessagesDB
        get() = mMessagesDB

    // # region end

    /**
     * Initialize the socket
     */
    fun initSocket(context: Context, listener: IServerListener) {
        mSocket = TSocket(context, listener, invoker)
    }

    class TSocket(
        private val mContext: Context,
        private val mSocketListener : IServerListener,
        private val invoker: String
    ) {
        private val isSocketServiceRunningOnStart = Utility.isMyServiceRunning(mContext, SocketService::class.java)

        private var mSocketService: Messenger? = null
        private var mIsConnected = false
        var connected: Boolean
            get() = mIsConnected
            set(value) {mIsConnected=value}

        companion object {
            const val SERVICE = 0
            const val UI = 1
        }

        private val mCallHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    // called when the client is connected to the server
                    SocketService.CONNECTED_CODE -> {
                        connected = true
                        mSocketListener.onConnect(true)
                    }
                    // called when the client connection is error
                    SocketService.CONN_ERROR_CODE -> {
                        connected = false
                        mSocketListener.onConnectError()
                    }
                    // called to close the application
                    SocketService.CLOSE_TMESSAGER -> {
                        mSocketListener.closeApplication()
                    }
                    // called to know if we are already connected to the server or not
                    SocketService.IS_CONNECTED -> {
                        val _connected = msg.data.getString("data").toBoolean()
                        if (!_connected) {
                            // connect to the server
                            val toSendMsg = Message.obtain(null, SocketService.CONNECT_CODE)
                            mSocketService?.send(toSendMsg)
                            connected = true
                        } else {
                            connected = true
                            // directly call the onConnect() if the socket service is already running
                            // we don't need to connect to the server cuz we are already connected
                            mSocketListener.onConnect(false)

                            // we need to set from parameter to empty so we can
                            // get a response back from the other peer if online
                            pingChatMate("")
                        }
                    }
                    // called to know if the client already connected to the chat room
                    SocketService.ROOM_ENTERED_CODE -> {
                        val it = Gson().fromJson(
                            msg.data.getString("data"),
                            TypeToken.get(Array<Any>::class.java)
                        )
                        mSocketListener.onEnteredRoom(it)
                    }
                    // called when the other peer disconnects to the server
                    SocketService.OFFLINE_CODE -> {
                        val it = Gson().fromJson(
                            msg.data.getString("data"),
                            TypeToken.get(Array<Any>::class.java)
                        )

                        mSocketListener.onDisconnect(it)
                    }
                    // called when the other peer joined the chat room
                    SocketService.CHATMATE_JOINED_CODE -> {
                        val it = Gson().fromJson(
                            msg.data.getString("data"),
                            TypeToken.get(Array<Any>::class.java)
                        )

                        mSocketListener.onUserJoined(it)
                    }
                    // called when there's new message
                    SocketService.NEW_MSG_CODE -> {
                        val it = Gson().fromJson(
                            msg.data.getString("data"),
                            TypeToken.get(Array<Any>::class.java)
                        )

                        mSocketListener.onNewMessage(it)
                    }
                    // called when the client want to send a message
                    SocketService.SEND_MSG_CODE -> {
                        val map = Gson().fromJson(
                            msg.data.getString("data"),
                            TypeToken.get(HashMap<String, Any>().javaClass)
                        )

                        mSocketListener.sendMessage(map)
                    }
                    // called when the message was already sent
                    SocketService.SENT_CODE -> {
                        val it = Gson().fromJson(
                            msg.data.getString("data"),
                            TypeToken.get(Array<Any>::class.java)
                        )

                        mSocketListener.onMessageSent(it)
                    }
                    // called whhen the message from the client was received
                    SocketService.RECEIVE_CODE -> {
                        val it = Gson().fromJson(
                            msg.data.getString("data"),
                            TypeToken.get(Array<Any>::class.java)
                        )

                        mSocketListener.onMessageReceived(it)
                    }
                    else -> {
                        super.handleMessage(msg)
                    }
                }
            }
        }

        private val mSocketServiceConn = object: ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                mSocketService = Messenger(service)
                sendToSocketService(SocketService.REGISTER_CLIENT_CODE)

                // we don't want to close the bubble chat if the invoker itself is the bubble chat
                if (invoker.contains("MainActivity")) {
                    closeBubbleChat()
                }

                sendToSocketService(SocketService.IS_CONNECTED)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                mSocketService = null
            }
        }

        private val mMessenger = Messenger(mCallHandler)

        init {
            _initSocketService()
        }

        private fun _initSocketService() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!isSocketServiceRunningOnStart)
                        mContext.startForegroundService(Intent(mContext, SocketService::class.java))

                    bind()
                } else {
                    if (!isSocketServiceRunningOnStart)
                        mContext.startService(Intent(mContext, SocketService::class.java))

                    bind()
                }
            }
        }

        /**
         * Sends an action to the socket service
         */
        fun sendToSocketService(code: Int, it: String? = null) {
            val msg = Message.obtain(null, code)
            msg.replyTo = mMessenger

            if (it == null) {
                mSocketService?.send(msg)
                return
            }

            msg.data = Bundle().apply {
                putString("data", it)
            }
            mSocketService?.send(msg)
        }

        /**
         * Sends message to the other peer
         * @param from who is the invoker - does the invoker is from the SERVICE? or UI ?
         * @param _msg The message to be sent
         * @param position The position of the message on the recyclerview.
         * @param status the status of the message
         * @param type the type of the message
         */
        fun sendMessage(
            from: Int,
            id: String,
            _msg : String,
            position: Int,
            status: String,
            type: com.dareangel.tmessager.data.model.Message.Companion.Type =
                com.dareangel.tmessager.data.model.Message.Companion.Type.SEND
        ) {
            val map = HashMap<String, Any>().apply {
                put("from", from)
                put("id", id)
                put("msg", _msg)
                put("pos", position)
                put("type", type)
                put("status", status)
            }

            sendToSocketService(SocketService.SEND_MSG_CODE, Gson().toJson(map))
        }

        /**
         * Ping the chat mate that the client is also online
         */
        fun pingChatMate(from: String = ChatHandler.USER) {
            sendToSocketService(SocketService.PING_CODE, from)
        }

        /**
         * Removes the messenger
         */
        private fun removeMessenger() {
            sendToSocketService(SocketService.UNREGISTER_CLIENT_CODE)
        }

        /**
         * Sends a received message action to the socket service
         */
        fun messageReceived(msg: String) {
            sendToSocketService(SocketService.RECEIVE_CODE, msg)
        }

        /**
         * Sends a delete unseen message action to the socket service
         */
        fun deleteUnseenMsgs() {
            sendToSocketService(SocketService.DELETE_MSGS_CODE)
        }

        /**
         * Sends an action to open the bubble chat
         */
        fun openBubbleChat() {
            sendToSocketService(SocketService.OPEN_BUBBLECHAT)
        }

        /**
         * Sends an action to close bubble chat
         */
        fun closeBubbleChat() {
            sendToSocketService(SocketService.CLOSE_BUBBLECHAT)
        }

        /**
         * Sends an action to schedule a service destroy
         */
        fun scheduleServiceDestroy() {
            sendToSocketService(SocketService.SCHEDULE_DESTROY_SERVICE)
        }

        /**
         * Sends an action to unschedule a service destroy
         */
        fun unscheduleServiceDestroy() {
            sendToSocketService(SocketService.UNSCHEDULE_DESTROY_SERVICE)
        }

        /**
         * Unbind from the service
         */
        fun unBind() {
            try {
                removeMessenger()
                mContext.unbindService(mSocketServiceConn)
            } catch (_:Exception) {}
        }

        /**
         * Bind to the service
         */
        fun bind() {
            mContext.bindService(
                Intent(
                    mContext,
                    SocketService::class.java
                ), mSocketServiceConn, Context.BIND_AUTO_CREATE
            )
        }
    }
}