package com.dareangel.tmessager.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Messenger
import android.widget.Toast
import com.dareangel.tmessager.db.ChildEventListenerAdapter
import com.dareangel.tmessager.db.Database
import com.dareangel.tmessager.interfaces.MessageListener
import com.dareangel.tmessager.`object`.User
import com.dareangel.tmessager.util.Utility
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class Message(private val cn: Context) {

    private val connectivityManager = cn.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val db : DatabaseReference = Database.getDatabaseRef()!!
    private val msgRef = db.child("Messages")
    private val unseenMsgsRef = db.child(if (USER == User.RENE) "Unseen_msgs_from_${User.EZIEL}" else "Unseen_msgs_from_${User.RENE}")
    private val userMsgsRef = db.child("Unseen_msgs_from_$USER")

    companion object {
        const val MAX_MSG_TO_LOAD = 20
        val USER = User.RENE
    }

    private var fromLostConn = false
    private var firstKey = ""

    var msgListener : MessageListener? = null
    // we need the messenger to determine if the user closed the app or not
    // we wouldn't want to send a message to the UI if the user is not online
    // to not make the app ping the other user that the message was seen when it is not
    // since the user closed the app and only the service is running
    var uiMessenger : Messenger? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (fromLostConn) {
                fromLostConn = false

                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    // Your code here
                    fetchMessages()
                }, 5000)
            }
        }

        override fun onLost(network: Network) {
            // Network is unavailable
            println("onLost")
            fromLostConn = true
        }
    }

    init {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }

        initListeners()
    }

    private fun initListeners() {
        // use to listen only for new messages, not the existing ones
        val msgQuery = msgRef.orderByChild("timestamp").startAt(System.currentTimeMillis().toDouble())

        msgQuery.addChildEventListener(object : ChildEventListenerAdapter() {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (uiMessenger == null) return

                val value = snapshot.getValue(MessageData::class.java)!!

                if (value.sender != USER && value.status == MessageData.STATUS_SENDING) {
                    value.status = MessageData.STATUS_SEEN

                    msgRef.child(value.id).setValue(value)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val msg = snapshot.getValue(MessageData::class.java)!!

                // when the user is not opening the app, we don't want to update the message status to seen
                // we just want to update the message badge of the floating view.
                if (uiMessenger == null) {
                    if (msg.sender != USER && msg.status == MessageData.STATUS_SENT) {
                        msgListener?.onNewMessage(msg)
                    }

                    return
                }

                if (msg.sender != USER && msg.status == MessageData.STATUS_SENT) {
                    msg.status = MessageData.STATUS_SEEN
                    msgRef.child(msg.id).setValue(msg)

                    msgListener?.onNewMessage(msg)
                } else if (msg.sender != USER && msg.status == MessageData.STATUS_SEEN) {
                    msgListener?.onNewMessage(msg)
                } else if (msg.sender == USER && msg.status == MessageData.STATUS_SEEN) {
                    msgListener?.onMessageSeen(msg)
                }
            }
        })
    }

    fun fetchMessages() {
        Utility.pingInternet { hasInternet ->
            fromLostConn = !hasInternet
        }

        msgRef.orderByKey().limitToLast(MAX_MSG_TO_LOAD).get()
        .addOnSuccessListener { _dataSnapshot ->
            val msgRefValue =
                ArrayList(_dataSnapshot.children.map { it.getValue(MessageData::class.java)!! })
            val snapshot = _dataSnapshot.children.firstOrNull()
            if (snapshot != null) {
                firstKey = snapshot.key!!
            }

            // if the message has sent status, seen it and update the database
            msgRefValue.forEach {

                if (it.sender != USER && it.status == MessageData.STATUS_SENT) {
                    it.status = MessageData.STATUS_SEEN

                    msgRef.child(it.id).setValue(it)
                }
            }

            msgListener?.onFetchMessages(msgRefValue)
        }
    }

    fun fetchUnseenMessages() {
        unseenMsgsRef.orderByKey().get()
            .addOnSuccessListener { dataSnapshot ->
                val value = dataSnapshot.children.map { it.getValue(MessageData::class.java)!! }

                value.forEach {
                    it.status = MessageData.STATUS_SEEN

                    unseenMsgsRef.child(it.id).removeValue()
                    msgRef.child(it.id).setValue(it)
                }
            }
    }

    fun loadMoreMessages() {
        if (firstKey.isEmpty()) return

        // fetch first from the local disk
        msgRef.orderByKey().endBefore(firstKey).limitToLast(MAX_MSG_TO_LOAD).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value = dataSnapshot.children.map { it.getValue(MessageData::class.java)!! }

                val snap = dataSnapshot.children.firstOrNull()
                if (snap != null) {
                    firstKey = snap.key!!
                    msgListener?.onFetchMessagesFromLoadMore(ArrayList(value))
                } else {
                    msgListener?.onFetchMessagesFromLoadMore()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    @Suppress("UNCHECKED_CAST")
    fun send(msg: String) {

        val message = MessageData(
            id = userMsgsRef.push().key!!,
            sender = USER,
            message = msg,
            timestamp = System.currentTimeMillis(),
            status = MessageData.STATUS_SENDING
        )

        msgListener?.onMessageSending(message)

        // send messages
        msgRef.child(message.id).setValue(message)
            .addOnSuccessListener {
                message.status = MessageData.STATUS_SENT

                msgRef.child(message.id).setValue(message)
                msgListener?.onMessageSent(message)
            }
    }
}