package com.dareangel.tmessager.data.database

object DBConstants {
    val TABLE_NAME = "RENE_TABLE"

    // COLUMNS
    val ID = "ID"
    val POSITION = "POSITION" // message position in the database
    val MESSAGE = "MESSAGE"
    val SENDER = "SENDER" // user or chat mate
    val STATUS = "STATUS" // sent or unsent

    val CREATE_MSGS_TABLE = "create table $TABLE_NAME " +
            "(" +
            "$POSITION INTEGER PRIMARY KEY, " +
            "$ID TEXT NOT NULL, " +
            "$MESSAGE TEXT NOT NULL, " +
            "$SENDER TEXT NOT NULL, " +
            "$STATUS TEXT NOT NULL" +
            ");"
}