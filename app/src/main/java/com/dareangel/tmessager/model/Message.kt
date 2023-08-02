package com.dareangel.tmessager.model

import android.os.Messenger
import com.dareangel.tmessager.db.ChildEventListenerAdapter
import com.dareangel.tmessager.db.Database
import com.dareangel.tmessager.interfaces.MessageListener
import com.dareangel.tmessager.`object`.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class Message {

    private val db : DatabaseReference = Database.getDatabaseRef()!!
    private val userRef = db.child("Users")
    private val msgRef = db.child("Messages")
    private val unseenMsgsRef = db.child(if (USER == User.RENE) "Unseen_msgs_from_${User.EZIEL}" else "Unseen_msgs_from_${User.RENE}")
    private val userMsgsRef = db.child("Unseen_msgs_from_$USER")

    companion object {
        const val MAX_MSG_TO_LOAD = 20
        val USER = User.RENE
    }

    private var firstKey = ""

    var msgListener : MessageListener? = null
    // we need the messenger to determine if the user closed the app or not
    // we wouldn't want to send a message to the UI if the user is not online
    // to not make the app ping the other user that the message was seen when it is not
    // since the user closed the app and only the service is running
    var uiMessenger : Messenger? = null

    init {
        // use to listen only for new messages, not the existing ones
        val unseenMsgsQuery = unseenMsgsRef.orderByChild("timestamp").startAt(System.currentTimeMillis().toDouble())

        userMsgsRef.addChildEventListener(object : ChildEventListenerAdapter() {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val value = snapshot.getValue(MessageData::class.java)!!
                value.status = MessageData.STATUS_SENT

                userMsgsRef.child(value.id).setValue(value)
                msgListener?.onMessageSent(value)
            }
        })

        unseenMsgsQuery.addChildEventListener(object : ChildEventListenerAdapter() {
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val value = snapshot.getValue(MessageData::class.java)!!

                if (uiMessenger != null) {
                    if (value.status == MessageData.STATUS_SENT) {
                        value.status = MessageData.STATUS_SEEN

                        unseenMsgsRef.child(value.id).removeValue()
                        msgRef.child(value.id).setValue(value)
                    }
                }
            }
        })

        msgRef.addChildEventListener(object : ChildEventListenerAdapter() {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val value = snapshot.getValue(MessageData::class.java)!!

                if (value.sender != USER) {
                    msgListener?.onNewMessage(value)
                } else {
                    msgListener?.onMessageSeen(value)
                }
            }
        })
    }

    /**
     * TODO: handle no network when sending message
     */

    fun fetchMessages() {
        msgRef.orderByKey().limitToLast(MAX_MSG_TO_LOAD).get()
        .addOnSuccessListener { _dataSnapshot ->
            val _value = _dataSnapshot.children.map { it.getValue(MessageData::class.java)!! }
            val snapshot = _dataSnapshot.children.firstOrNull()
            if (snapshot != null) {
                firstKey = snapshot.key!!
            }

            msgListener?.onFetchMessages(ArrayList(_value))
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
        userMsgsRef.child(message.id).setValue(message)
//            .addOnSuccessListener {
//                message.status = MessageData.STATUS_SENT
//                msgRef.child(message.id).setValue(message)
//                    .addOnSuccessListener {
//                        msgListener?.onMessageSent(message)
//                    }
//                    .addOnFailureListener {
//                        msgListener?.onMessageFailed()
//                    }
//            }
    }
}