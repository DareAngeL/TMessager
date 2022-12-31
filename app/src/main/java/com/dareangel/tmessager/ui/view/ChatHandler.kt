package com.dareangel.tmessager.ui.view

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import com.dareangel.tmessager.R
import com.dareangel.tmessager.data.database.SocketService
import com.dareangel.tmessager.data.model.Message
import com.dareangel.tmessager.manager.DataManager
import com.dareangel.tmessager.ui.sound.SoundPlayer
import com.dareangel.tmessager.ui.view.messagesdisplayer.MessagesAdapter
import com.dareangel.tmessager.ui.view.messagesdisplayer.MessagesRecyclerview2
import com.dareangel.tmessager.util.Utility
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import io.socket.emitter.Emitter
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

class ChatHandler(
    private val mContext: Context
) {

    private val mCoroutineScope = CoroutineScope(Job() + Dispatchers.Default)
    private val mSoundPlayer = SoundPlayer(mContext)

    private var mDataManager: DataManager? = null
    var dataManager: DataManager?
        get() = mDataManager
        set(value) {
            mDataManager = value
        }

    companion object {
        val USER = "Babe Eziel"
    }

    private var mMessagesAdapter: MessagesAdapter? = null
    var messagesAdapter: MessagesAdapter?
        get() = mMessagesAdapter
        set(value) {mMessagesAdapter = value}

    private var mAdapterItemListener: MessagesAdapter.ItemListener? = null
    var adapterItemListener: MessagesAdapter.ItemListener?
        get() = mAdapterItemListener
        set(value) {mAdapterItemListener=value}

    private var mConnStatusView: ImageView? = null
    var connStatusView: ImageView?
        get() = mConnStatusView
        set(value) {mConnStatusView=value}

    private var mMsgRecyclerView: MessagesRecyclerview2? = null
    var msgRecyclerview: MessagesRecyclerview2?
        get() = mMsgRecyclerView
        set(value) {mMsgRecyclerView=value}

    fun initialize(onMessageFetch: () -> Unit) {
        fetchMessages {
            onMessageFetch.invoke()
        }
    }

    fun fetchMessages(onMessageFetch: () -> Unit) {
        mDataManager!!.msgsDBTable.apply {
            fetchMessages {
                mCoroutineScope.launch(Dispatchers.Main) {
                    // on finish fetching messages from the local database
                    // declare the recyclerview's adapter
                    mMessagesAdapter = MessagesAdapter(mContext, USER, it, mAdapterItemListener!!)
                    onMessageFetch.invoke()
                }
            }
        }
    }

    private fun updateChatmateConnStatus(connType: Utility.CONN_TYPE) {
        if (connStatusView == null)
            return

        if (connType == Utility.CONN_TYPE.ONLINE) {
            connStatusView?.setImageDrawable(
                AppCompatResources.getDrawable(mContext, R.drawable.status_online_ind)
            )
        } else {
            connStatusView?.setImageDrawable(
                AppCompatResources.getDrawable(mContext, R.drawable.status_offline_ind)
            )
        }
    }

    private fun _addMessageFromChatmate(msgJson: JSONObject, isAddToDB: Boolean = true) {
        addMessage("", msgJson.getString("MSG"), "", isAddToDB)
    }

    fun addMessage(sender: String, msg: String, status: String?, isAddToDB: Boolean) {
        val pos = mMessagesAdapter!!.rawDataSize
        val _msg = Message(msg, sender, pos, status)

        mMessagesAdapter!!.appendMessage(mDataManager!!, _msg, isAddToDB)
        msgRecyclerview!!.smoothScrollToPosition(mMessagesAdapter!!.itemCount-1)
    }

    fun onDisconnect() {
        mCoroutineScope.launch(Dispatchers.Main) {
            updateChatmateConnStatus(Utility.CONN_TYPE.OFFLINE)
        }
    }

    fun onConnectError() {
        mCoroutineScope.launch(Dispatchers.Main) {
            Toast.makeText(mContext, "Connection Lost", Toast.LENGTH_SHORT).show()
        }
    }

    private val mUnseenMessages = ArrayList<JSONObject>()
    fun onNewMessage(
        it: Array<Any>,
        isSeen: Boolean = true,
        onUnseenMsgs: (count: Int) -> Unit = {}
    ) {
        mCoroutineScope.launch(Dispatchers.Main) {
            val msgStr = it[0] as String
            val msgJson = JSONObject(msgStr)

            if (isSeen) {
                _addMessageFromChatmate(msgJson)
                mDataManager?.socket?.sendToSocketService(SocketService.SET_UNSEEN_MSGS_JSON, "")
                mDataManager?.socket?.messageReceived(msgStr)
            } else {
                // stores the messages from the other peer.
                mUnseenMessages.add(msgJson)
                val map = HashMap<String, Any>().apply {
                    put("inStr", msgStr)
                    put("inList", mUnseenMessages)
                }

                mDataManager?.socket?.sendToSocketService(
                    SocketService.SET_UNSEEN_MSGS_JSON,
                    Gson().toJson(map)
                )

                onUnseenMsgs.invoke(mUnseenMessages.size)
            }
        }
    }

    /**
     * Seen a message from the other peer
     */
    fun messageSeen() {
        val unseenMsgsMap = mDataManager?.socket!!.unseenMsgsMap
        if (unseenMsgsMap.isEmpty())
            return

        val list = unseenMsgsMap["inList"] as ArrayList<*>
        // parse the data and load the unseen messages from the other peer
        list.forEach {
            val tree = it as LinkedTreeMap<*, *>
            val jsonObj = JSONObject()
            tree.forEach { (_, any2) ->
                val value = any2 as LinkedTreeMap<*, *>
                val keys = value.keys

                jsonObj.put(keys.elementAt(0) as String, value[keys.elementAt(0)])
                jsonObj.put(keys.elementAt(1) as String, value[keys.elementAt(1)])
            }
            _addMessageFromChatmate(jsonObj)
            // add to the local database
//            mDataManager!!.msgsDBTable.addMessage(
//                Message(
//                    jsonObj.getString("MSG"),
//                    "",
//                    mMessagesAdapter!!.rawDataSize,
//                    ""
//                )
//            )
        }

        mDataManager?.socket?.messageReceived(unseenMsgsMap["inStr"] as String)
        mDataManager?.socket?.sendToSocketService(SocketService.SET_UNSEEN_MSGS_JSON, "")
        // clear the unseen messages
        mUnseenMessages.clear()
    }

    fun onMessageSent(it: Array<Any>) {
        mCoroutineScope.launch(Dispatchers.Main) {
            // plays sound effect
            mSoundPlayer.playSentSound()
            // would return the position of the message that was sent
            // then update the message status in the adapter according to its position
            val msgJSON = JSONObject(it[0] as String)
            // the position of the message at the adapter level
            val pos = msgJSON.getString("POS").toInt()
            // gets the position of the message at the data level
            val posAtDataLevel = mMessagesAdapter?.getPosition(pos, MessagesAdapter.PositionType.DATA_POSITION)!!
            // this fixes the {OutOfBoundsIndex} exception
            if ((mMessagesAdapter!!.itemCount-1) - (posAtDataLevel) != 1 &&
                mMessagesAdapter!!.itemCount-1 <= posAtDataLevel)
                return@launch

            val msg = mMessagesAdapter?.updateMessageStatus(pos, Message.SENT)
            mDataManager!!.msgsDBTable.updateMessageStatus(msg!!)
        }
    }

    fun onMessageReceived(it: Array<Any>) {
        mCoroutineScope.launch(Dispatchers.IO) {
            // it -> contains the unseen messages of the client
            // we need to update their status on the adapter as
            // soon as the messages arrived to the other peer.
            try {
                // try to parse the {it} to JSONArray
                val msgJSONArr = JSONArray(it[0] as String)
                for (i in 0 until msgJSONArr.length()) {
                    val msgJSON = msgJSONArr.getJSONObject(i)
                    withContext(Dispatchers.Main) {
                        updateAdapterOnReceived(msgJSON)
                    }
                }
                // after updating the adapter, remove the unseen msgs at the server
                mDataManager!!.socket?.deleteUnseenMsgs()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateAdapterOnReceived(JSONObject(it[0] as String))
                }
                mDataManager!!.socket?.deleteUnseenMsgs()
            }
        }
    }

    fun updateAdapterOnReceived(msgJSON: JSONObject) {
        val pos = msgJSON.getString("POS").toInt()
        // gets the position of the message at the data level
        val posAtDataLevel = mMessagesAdapter?.getPosition(pos, MessagesAdapter.PositionType.DATA_POSITION)!!

        // this fixes the {OutOfBoundsIndex} exception - FOR NOW
        if ((mMessagesAdapter!!.itemCount-1) - (posAtDataLevel) != 1 &&
            mMessagesAdapter!!.itemCount-1 <= posAtDataLevel)
            return

        val msg = mMessagesAdapter?.updateMessageStatus(pos, Message.SEEN)
        mDataManager!!.msgsDBTable.updateMessageStatus(msg!!)
    }

    fun onUserJoined(it: Array<Any>) {
        mCoroutineScope.launch(Dispatchers.Main) {
            // when the other peer joined the room while the client is online, update the
            // connection status of the client's other peer.
            val from = it[0] as String
            updateChatmateConnStatus(Utility.CONN_TYPE.ONLINE)
            // and ping the other peer that the client is also online
            if (from.isEmpty())
                mDataManager!!.socket!!.pingChatMate()
        }
    }

    // we need this, because onEnteredRoom might be called
    // multiple times and we don't want to have a duplicate message
    private var prevUnseenMsgPOS = -1
    fun onEnteredRoom(it: Array<Any>) {
        checkUnseenMessagesFromServer(it)
    }

    fun checkUnseenMessagesFromServer(it: Array<Any>) {
        mCoroutineScope.launch(Dispatchers.IO) {
            val msgs = JSONObject(it[0] as String)  // JSONArray(it[0] as String)
            val reneUnseenMsgs = msgs.getJSONArray("Rene")
            val ezielUnseenMsgs = msgs.getJSONArray("Eziel")

            if (USER.contains("Rene")) {
                if (reneUnseenMsgs.length() > 0)
                    onMessageReceived(arrayOf(reneUnseenMsgs.toString()))
            } else {
                if (ezielUnseenMsgs.length() > 0)
                    onMessageReceived(arrayOf(ezielUnseenMsgs.toString()))
            }

            val chatmateUnseenMsgs: JSONArray = if (USER.contains("Rene")) {
                ezielUnseenMsgs
            } else {
                reneUnseenMsgs
            }

            if (chatmateUnseenMsgs.length() > 0) {
                for (i in 0 until chatmateUnseenMsgs.length()) {
                    val msgObj = chatmateUnseenMsgs[i] as JSONObject
                    if (msgObj.getInt("POS") == prevUnseenMsgPOS)
                    {
                        continue
                    }
                    prevUnseenMsgPOS = msgObj.getInt("POS")

                    withContext(Dispatchers.Main) {
                        _addMessageFromChatmate(msgObj)
                        mMessagesAdapter!!.notifyChanges(Message.SECOND_LAST_MSG)
                    }
                }
                mDataManager!!.socket!!.messageReceived(chatmateUnseenMsgs.toString())
            }
        }
    }

    fun onTyping(): Emitter.Listener {
        TODO("Not yet implemented")
    }

    fun onStopTyping(): Emitter.Listener {
        TODO("Not yet implemented")
    }
}