package com.dareangel.tmessager.model

import com.dareangel.tmessager.db.Database
import com.dareangel.tmessager.interfaces.MessageListener
import com.dareangel.tmessager.`object`.User
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class Message {

    private val db : DatabaseReference = Database.getDatabaseRef()!!
    private val userRef = db.child("Users")
    private val msgRef = db.child("Messages")

    companion object {
        const val MAX_MSG_TO_LOAD = 20
        val USER = User.EZIEL
    }

    private var firstKey = ""

    var msgListener : MessageListener? = null

    init {
        // use to listen only for new messages, not the existing ones
        val query = msgRef.orderByChild("timestamp").startAt(System.currentTimeMillis().toDouble())

        query.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                val value = dataSnapshot.getValue(MessageData::class.java)!!

                if (value.sender != USER) {
                    msgListener?.onNewMessage(value)
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                val value = dataSnapshot.getValue(MessageData::class.java)!!

                if (value.seenBy.isNotEmpty() && value.seenBy != USER) {
                    msgListener?.onMessageSeen(value)
                }
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    fun fetchMessages() {
        msgRef.orderByKey().limitToLast(MAX_MSG_TO_LOAD).get()
            .addOnSuccessListener { dataSnapshot ->
                val value = dataSnapshot.children.map { it.getValue(MessageData::class.java)!! }
                val snapshot = dataSnapshot.children.firstOrNull()
                if (snapshot != null) {
                    firstKey = snapshot.key!!
                }

                // change all msgs that have sent status to seen
                value.filter { it.sender != USER && it.status == MessageData.STATUS_SENT }.forEach {
                    it.seenBy = USER
                    msgRef.child(it.id).setValue(it)
                }

                msgListener?.onFetchMessages(ArrayList(value))
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
            id = msgRef.push().key!!,
            sender = USER,
            message = msg,
            timestamp = System.currentTimeMillis(),
            status = MessageData.STATUS_SENDING
        )

        msgListener?.onMessageSending(message)
        // send messages
        msgRef.child(message.id).setValue(message).apply {
            addOnCompleteListener {
                if (it.isSuccessful) {
                    message.status = MessageData.STATUS_SENT
                    msgListener?.onMessageSent(message)
                } else {
                    msgListener?.onMessageFailed()
                }
            }

            addOnFailureListener {
                msgListener?.onMessageFailed()
            }
        }
    }
}