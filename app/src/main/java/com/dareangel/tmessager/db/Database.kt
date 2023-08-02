package com.dareangel.tmessager.db

import android.content.Context
import com.dareangel.tmessager.R
import com.dareangel.tmessager.model.Message
import com.dareangel.tmessager.model.MessageData
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

object Database {

    private var db : FirebaseDatabase? = null
    private var dbRef : DatabaseReference? = null
    private var userActiveRef : DatabaseReference? = null

    private var mTotalOnline = 0

    var totalOnline : Int
        get() = mTotalOnline
        set(value) {
            mTotalOnline = value
        }

    fun initialize(context: Context) {

        if (db != null) {
            return
        }

        // initialize database
        db = Firebase.database(context.getString(R.string.db_ref))
        db?.setPersistenceEnabled(true)
        // sync
        dbRef = db?.reference
        userActiveRef = dbRef?.child("Active")

        listeners()
    }

    private fun listeners() {
        userActiveRef!!.addChildEventListener(object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                val value = snapshot.getValue(String::class.java)!!
            }

            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {
                val value = snapshot.getValue(String::class.java)!!
            }

            override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}

            override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    /**
     * Goes online
     */
    fun online() {
        userActiveRef!!.child("user_${Message.USER}").setValue(Message.USER)
    }

    /**
     * Goes offline
     */
    fun offline() {
        userActiveRef!!.child("user_${Message.USER}").removeValue()
    }

    fun getDatabaseRef() : DatabaseReference? {
        return dbRef
    }

    fun updateMessage(message: MessageData) {
        dbRef?.child("Messages")?.child(message.id)?.setValue(message)
    }
}