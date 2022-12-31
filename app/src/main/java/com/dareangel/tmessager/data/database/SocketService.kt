package com.dareangel.tmessager.data.database

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.dareangel.tmessager.R
import com.dareangel.tmessager.ui.view.ChatHandler
import com.dareangel.tmessager.ui.view.bubblechat.BubbleChatWindow
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.timerTask

@Suppress("LocalVariableName", "PrivatePropertyName", "FunctionName")
class SocketService: Service() {

    private var mConnected = false
    // keys: inStr, inList
    private var mUnseenMsgsMap = ""

    private var mBubbleChatWindow : BubbleChatWindow? = null
    private var mTimer: Timer? = null
    private lateinit var mTimerTask : TimerTask

    private val mClients = ArrayList<Messenger>()
    private lateinit var mSocket : Socket

    private lateinit var ROOM_NAME : String
    // the delay before destroying the service
    private val DELAY_DESTROY_VALUE = 900000L // 15 minutes
    private val CLOSE_ACTION = "CLOSE"

    companion object {
        const val ENTER_ROOM = "enter-chatroom"
        const val OFFLINE = "offline"
        const val SEND_MSG = "send_msg"
        const val NEW_MSG = "new-msg"
        const val SENT = "msg_sent"
        const val RECEIVE = "rcv"
        const val DEL_UNSEEN = "delete_unseen_msgs"
        const val CHATMATE_JOINED = "chat-mate-joined"
        const val GET_UNSEEN = "getunseen"

        // below are the flags for the socket service
        const val REGISTER_CLIENT_CODE = 1
        const val UNREGISTER_CLIENT_CODE = 2
        const val CONNECT_CODE = 3
        const val CONNECTED_CODE = 4
        const val CONN_ERROR_CODE = 5
        const val ROOM_ENTERED_CODE = 6
        const val OFFLINE_CODE = 7
        const val CHATMATE_JOINED_CODE = 8
        const val SEND_MSG_CODE = 9
        const val NEW_MSG_CODE = 10
        const val SENT_CODE = 11
        const val RECEIVE_CODE = 12
        const val PING_CODE = 13
        const val OPEN_BUBBLECHAT = 14
        const val CLOSE_BUBBLECHAT = 15
        const val DELETE_MSGS_CODE = 16
        const val SCHEDULE_DESTROY_SERVICE = 17
        const val UNSCHEDULE_DESTROY_SERVICE = 18
        const val IS_CONNECTED = 19
        const val SET_UNSEEN_MSGS_JSON = 20
        const val GET_UNSEEN_MSGS_JSON = 21
        const val CLOSE_TMESSAGER = 22
    }

    private val mCallHandler = object: Handler(Looper.getMainLooper()) {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun handleMessage(msg: Message) {

            when (msg.what) {
                REGISTER_CLIENT_CODE -> {
                    if (!mClients.contains(msg.replyTo))
                        mClients.add(msg.replyTo)
                }
                UNREGISTER_CLIENT_CODE -> {
                    mClients.remove(msg.replyTo)
                }
                SET_UNSEEN_MSGS_JSON -> {
                    mUnseenMsgsMap = msg.data.getString("data")!!
                }
                GET_UNSEEN_MSGS_JSON -> {
                    toStringSend(GET_UNSEEN_MSGS_JSON, mUnseenMsgsMap)
                }
                CONNECT_CODE -> {
                    connectToServer()
                }
                IS_CONNECTED -> {
                    toJSONSend(IS_CONNECTED, mConnected)
                }
                CLOSE_TMESSAGER -> {
                    toStringSend(CLOSE_TMESSAGER)
                }
                SEND_MSG_CODE -> {
                    val map = Gson().fromJson(
                        msg.data.getString("data"),
                        TypeToken.get(HashMap<String, Any>().javaClass)
                    )

                    val pos = (map["pos"] as Double).toInt()
                    val _msg = map["msg"] as String
                    val status = map["status"] as String

                    // notify the clients that we are sending message to the other peer
                    toJSONSend(SEND_MSG_CODE, map)

                    // sends the message to the other peer
                    if (status != com.dareangel.tmessager.data.model.Message.NOT_SENT) {
                        sendMessage(_msg, pos+1)
                    }
                }
                DELETE_MSGS_CODE -> {
                    deleteUnseenMsgs()
                }
                ROOM_ENTERED_CODE -> {
                    mSocket.emit(ENTER_ROOM, ChatHandler.USER, ROOM_NAME)
                }
                RECEIVE_CODE -> {
                    messageReceived(msg.data.getString("data")!!)
                }
                PING_CODE -> {
                    pingChatMate(msg.data.getString("data")!!)
                }
                OPEN_BUBBLECHAT -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        openBubbleChat()
                    }
                }
                CLOSE_BUBBLECHAT -> {
                    closeBubbleChat()
                }
                SCHEDULE_DESTROY_SERVICE -> {
                    mTimer = Timer()
                    try {
                        mTimer?.schedule(mTimerTask, DELAY_DESTROY_VALUE)
                    } catch (_:Exception) {}
                }
                UNSCHEDULE_DESTROY_SERVICE -> {
                    mTimer?.cancel()
                }
                else -> {
                    super.handleMessage(msg)
                }
            }
        }
    }

    private val mMessenger: Messenger = Messenger(mCallHandler)

    override fun onBind(intent: Intent?): IBinder {
        return mMessenger.binder
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        // init timer task
        mTimerTask = timerTask {
            stop()
        }

        // init room name value
        ROOM_NAME = getString(R.string.room_name)

        // init the socket io
        try {
            mSocket = IO.socket(/*getString(R.string.server)*/"http://192.168.254.105:2022")
            _initSocketEvents()
        } catch (e: Exception) {
            throw Exception(e.message)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(1, buildNotification())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (action != null) {
                when (action) {
                    CLOSE_ACTION -> stop()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun openBubbleChat() {
        if (mBubbleChatWindow == null) {
            // init bubble chat window
            mBubbleChatWindow = BubbleChatWindow(this)
        }

        mBubbleChatWindow!!.open()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun closeBubbleChat() {
        mBubbleChatWindow?.unbind()
        mBubbleChatWindow?.close()

        mBubbleChatWindow = null
    }

    private fun _initSocketEvents() {
        mSocket.on(Socket.EVENT_CONNECT) {
            Handler(Looper.getMainLooper()).postDelayed({
                mConnected = true
                // tell the clients that it's connected, so they can logged in
                toJSONSend(CONNECTED_CODE)

                mSocket.emit(ENTER_ROOM, ChatHandler.USER, ROOM_NAME)
            }, 1000)
        }

        mSocket.on(Socket.EVENT_CONNECT_ERROR) {
            val errorBuilder = StringBuilder()
            mConnected = false

            for (i in it.indices) {
                errorBuilder.append(it[i])
            }

            toJSONSend(CONN_ERROR_CODE)
        }

        mSocket.on(ENTER_ROOM) {
            toJSONSend(ROOM_ENTERED_CODE, it)
        }

        mSocket.on(OFFLINE) {
            toJSONSend(OFFLINE_CODE, it)
        }

        mSocket.on(CHATMATE_JOINED) {
            toJSONSend(CHATMATE_JOINED_CODE, it)
        }

        mSocket.on(NEW_MSG) {
            toJSONSend(NEW_MSG_CODE, it)
        }
        mSocket.on(SENT) {
            toJSONSend(SENT_CODE, it)
        }
        mSocket.on(RECEIVE) {
            toJSONSend(RECEIVE_CODE, it)
        }
    }

    private fun toStringSend(code: Int, it: String? = null) {
        send(code, false, it)
    }

    private fun toJSONSend(code: Int, it: Any? = null) {
        send(code, true, it)
    }

    private fun send(code: Int, _toJSONSend: Boolean, it: Any? = null) {
        val clientsToRemove = ArrayList<Messenger>()

        if (it == null) {
            mClients.forEach { msger ->
                try {
                    msger.send(Message.obtain(null, code))
                } catch (e: Exception) {
                    clientsToRemove.add(msger)
                }
            }
            if (clientsToRemove.isNotEmpty()) {
                mClients.removeAll(clientsToRemove.toSet())
            }

            return
        }

        mClients.forEach { msger ->
            try {
                val m = Message.obtain(null, code)
                m.data = Bundle().apply {
                    if (_toJSONSend)
                        putString("data", Gson().toJson(it))
                    else
                        putString("data", it as String)
                }
                msger.send(m)
            } catch (e: Exception) {
                clientsToRemove.add(msger)
            }
        }
        if (clientsToRemove.isNotEmpty()) {
            mClients.removeAll(clientsToRemove.toSet())
        }
    }

    /**
     * Sends message to the other peer
     * @param msg The message to be sent
     * @param position The position of the message on the recyclerview.
     */
    fun sendMessage(msg : String, position: Int) {
        mSocket.emit(SEND_MSG, ROOM_NAME, ChatHandler.USER, msg, position.toString())
    }

    /**
     * Ping to the chat mate that client is also online
     */
    fun pingChatMate(from: String) {
        mSocket.emit(CHATMATE_JOINED, ROOM_NAME, from)
    }

    fun messageReceived(msg: String) {
        mSocket.emit(RECEIVE, ChatHandler.USER, ROOM_NAME, msg)
    }

    fun deleteUnseenMsgs() {
        mSocket.emit(DEL_UNSEEN, ChatHandler.USER, ROOM_NAME)
    }

    fun connectToServer() {
        mSocket.connect()
    }

    /**
     * Stop the service, the database and the socket
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun stop() {
        mBubbleChatWindow?.unbind() // unbinds the bubble chat from the socket service first
        toStringSend(CLOSE_TMESSAGER)
        closeSocket()
        stopSelf()
    }

    private fun closeSocket() {
        mSocket.emit(OFFLINE, ROOM_NAME)
        mSocket.disconnect()
        mSocket.off(Socket.EVENT_CONNECT)
        mSocket.off(ENTER_ROOM)
        mSocket.off(OFFLINE)
        mSocket.off(CHATMATE_JOINED)
        mSocket.off(SENT)
        mSocket.off(NEW_MSG)
        mSocket.off(RECEIVE)
        mSocket.close()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildNotification(): Notification {
        val NOTIFICATION_CHANNEL_ID = "socket.connection"
        val channelName = "TMessager"
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_MIN
        )

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val closeIntent = Intent(this, SocketService::class.java).also {
            it.action = CLOSE_ACTION
        }
        val closePendingIntent: PendingIntent =
            PendingIntent.getService(this, -1, closeIntent, PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        return notificationBuilder.setOngoing(true)
            .setContentTitle("TMessager is running")
            .setContentText("You are online")
            .setSmallIcon(R.drawable.appicon)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(R.drawable.ic_close, "CLOSE", closePendingIntent)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        mBubbleChatWindow!!.close()
        closeSocket()
        super.onDestroy()
    }
    // # region end
}