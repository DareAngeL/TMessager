package com.dareangel.tmessager.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.dareangel.tmessager.data.database.helpers.MessagesDBTableHelper
import com.dareangel.tmessager.data.model.Message
import com.dareangel.tmessager.data.model.interfaces.IMessagesTable
import kotlinx.coroutines.*

@Suppress("PrivatePropertyName")
class MessagesDB : IMessagesTable {

    private val mCoroutineScope = CoroutineScope(Job() + Dispatchers.Default)
    private lateinit var mDatabase : SQLiteDatabase

    private var mDBHelper : MessagesDBTableHelper? = null

    override fun open(cn: Context): IMessagesTable {
        mDBHelper = MessagesDBTableHelper(
            cn, MessagesDBTableHelper.DB_NAME, null, MessagesDBTableHelper.DB_VERSION
        )
        mDatabase = mDBHelper!!.writableDatabase
        return this
    }

    override fun close(alsoCloseCoroutine: Boolean) {
        if (alsoCloseCoroutine)
            mCoroutineScope.cancel()

        if (mDBHelper == null)
            return

        mDBHelper?.close()
    }

    override fun updateMessageStatus(msg: Message) {
        mCoroutineScope.launch(Dispatchers.IO) {
            ContentValues().apply {
                put(DBConstants.POSITION, msg.pos)
                put(DBConstants.MESSAGE, msg.msg)
                put(DBConstants.SENDER, msg.sender)
                put(DBConstants.STATUS, msg.status)

                mDatabase.update(
                    DBConstants.TABLE_NAME,
                    this,
                    DBConstants.POSITION + " = ${msg.pos}",
                    null
                )
            }
        }
    }

    override fun fetchMessages(listener: IMessagesTable.OnFetchingListener) {
        mCoroutineScope.launch(Dispatchers.IO) {
            arrayListOf<Message>().apply {
                val cursor = mDatabase.query(
                    DBConstants.TABLE_NAME,
                    arrayOf(
                        DBConstants.ID,
                        DBConstants.POSITION, DBConstants.MESSAGE,
                        DBConstants.SENDER, DBConstants.STATUS
                    ),
                    null,
                    null,
                    null,
                    null,
                    null
                )

                cursor.moveToFirst()
                for (i in 0 until cursor.count) {
                    this@apply.add(Message(
                        cursor.getString(cursor.getColumnIndexOrThrow(DBConstants.ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DBConstants.MESSAGE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DBConstants.SENDER)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DBConstants.POSITION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DBConstants.STATUS))
                    ))
                    cursor.moveToNext()
                }

                cursor.close()
                // callback when fetching is finish
                listener.onFinishedFetching(this)
            }
        }
    }

    /**
     * Add message to the local database
     * @param msg The message to be added
     */
    override fun addMessage(msg: Message) {
        mCoroutineScope.launch(Dispatchers.IO) {
            ContentValues().apply {
                put(DBConstants.POSITION, msg.pos)
                put(DBConstants.ID, msg.id)
                put(DBConstants.MESSAGE, msg.msg)
                put(DBConstants.SENDER, msg.sender)
                put(DBConstants.STATUS, msg.status)

                mDatabase.insertWithOnConflict(
                    DBConstants.TABLE_NAME,
                    null,
                    this,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
        }
    }
}
