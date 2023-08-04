package com.dareangel.tmessager.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import androidx.core.app.NotificationCompat
import carbon.widget.TextView
import com.dareangel.tmessager.R
import com.dareangel.tmessager.interfaces.MessageListener
import com.dareangel.tmessager.model.MessageData
import com.dareangel.tmessager.`object`.ForegroundNotification.CHANNEL_ID
import com.dareangel.tmessager.`object`.ForegroundNotification.NOTIFICATION_ID
import com.dareangel.tmessager.`object`.MessengerCodes
import com.dareangel.tmessager.presenter.MessagePresenter
import com.dareangel.tmessager.service.comm.IncomingDataFromUIHandler
import com.dareangel.tmessager.ui.animator.Animator
import com.dareangel.tmessager.ui.view.MainActivity
import com.dareangel.tmessager.ui.view.fragments.FloatingView
import com.dareangel.tmessager.util.Utility
import com.google.gson.Gson

class MessagingService : Service(), MessageListener {

    private lateinit var messagePresenter : MessagePresenter
    private lateinit var floatingView : FloatingView

    private val messenger = Messenger(IncomingDataFromUIHandler(this))
    private var uiMessenger: Messenger? = null

    override fun onBind(p0: Intent?): IBinder? {
        return messenger.binder
    }

    override fun onCreate() {
        super.onCreate()
        messagePresenter = MessagePresenter(this, this)
        floatingView = FloatingView(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create a notification and set its properties
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setOngoing(true)
            .setContentTitle("TMessager")
            .setContentText("TMessager is running...")
            .build()

        // Start the service in the foreground and show the notification
        startForeground(NOTIFICATION_ID, notification)

        // get the messenger from the intent
        uiMessenger = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("messenger", Messenger::class.java)
        } else {
            intent?.getParcelableExtra("messenger")
        }
        // set the messenger to the message presenter
        messagePresenter.setUIMessenger(uiMessenger!!)

        // hide the floating view
        floatingView.hide()

        return START_NOT_STICKY
    }

    /**
     * Sends message to other user
     */
    fun sendMessage(message: String) {
        messagePresenter.sendMessage(message)
    }

    /**
     * Fetches messages from the database
     */
    fun fetchMessages() {
        messagePresenter.fetchMessages()
    }

    /**
     * Fetches unseen messages from the database
     */
    fun fetchUnseenMessages() {
        messagePresenter.fetchUnseenMessages()
    }

    /**
     * Loads more messages from the database
     */
    fun loadMoreMessages() {
        messagePresenter.loadMoreMessages()
    }

    /**
     * Removes the messenger of the UI
     */
    fun removeMessengerUI() {
        uiMessenger = null
        messagePresenter.setUIMessenger(null)

        // if the messenger is null, it means that the user closed the app
        // let's show the floating view
        floatingView.show()
    }

    override fun onFetchMessages(messages: ArrayList<MessageData>) {
        sendDataToUI(MessengerCodes.FETCH_MSGS, messages)
    }

    override fun onFetchMessagesFromLoadMore(messages: ArrayList<MessageData>?) {
        sendDataToUI(MessengerCodes.LOAD_MORE_MSGS, messages)
    }

    override fun onMessageSent(msg: MessageData) {
        sendDataToUI(MessengerCodes.MSG_SENT, msg)
    }

    override fun onMessageSeen(msg: MessageData) {
        // if uiMessenger is null, it means that the user closed the app
        // so we don't need to send the message to the UI
        if (uiMessenger == null) {
            return
        }

        sendDataToUI(MessengerCodes.MSG_SEEN, msg)
    }

    override fun onMessageFailed() {
        sendDataToUI(MessengerCodes.MSG_FAILED)
    }

    override fun onNewMessage(message: MessageData) {
        floatingView.onNewMessage()
        if (uiMessenger == null) {
            return
        }

        sendDataToUI(MessengerCodes.NEW_MSG, message)
    }

    override fun onMessageSending(msg: MessageData) {
        sendDataToUI(MessengerCodes.MSG_SENDING, msg)
    }

    private fun sendDataToUI(code: Int, data: Any? = null) {
        if (uiMessenger == null) {
            return
        }

        val message = Message.obtain(null, code)

        if (data != null) {
            val bundle = Bundle()

            when (data) {
                is Boolean -> bundle.putBoolean("data", data)
                is String -> bundle.putString("data", data)
                is ArrayList<*> -> {
                    // convert the data to Gson()
                    val dataJSON = Gson().toJson(data)
                    bundle.putString("data", dataJSON)
                }
                is MessageData -> {
                    // convert the data to Gson()
                    val dataJSON = Gson().toJson(data)
                    bundle.putString("data", dataJSON)
                }
            }

            message.data = bundle
        }

        try {
            uiMessenger?.send(message)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }
}