package com.dareangel.tmessager.db

import android.content.Context
import com.dareangel.tmessager.R
import com.dareangel.tmessager.interfaces.DatabaseListener
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

    private var mDatabaseListener: DatabaseListener? = null
    var databaseListener: DatabaseListener?
        get() = mDatabaseListener
        set(value) {
            mDatabaseListener = value
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
        dbRef?.keepSynced(true)
        userActiveRef = dbRef?.child("Active")

        listeners()
    }

    private fun listeners() {
        userActiveRef!!.addChildEventListener(object : ChildEventListenerAdapter() {
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                val value = snapshot.getValue(String::class.java)!!

                if (value != Message.USER) {
                    databaseListener?.onOtherUserLoggedIn()
                }
            }

            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {
                val value = snapshot.getValue(String::class.java)!!

                if (value != Message.USER) {
                    databaseListener?.onOtherUserLoggedOut()
                }
            }
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
}