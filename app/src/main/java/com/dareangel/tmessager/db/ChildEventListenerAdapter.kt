package com.dareangel.tmessager.db

import com.google.firebase.database.ChildEventListener

abstract class ChildEventListenerAdapter : ChildEventListener {
    override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
    override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
    override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
    override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
}