package com.dareangel.tmessager.ui.view.fragments

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Rect
import android.os.*
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.dareangel.tmessager.R
import com.dareangel.tmessager.databinding.FragmentChatroomBinding
import com.dareangel.tmessager.db.Database
import com.dareangel.tmessager.interfaces.DatabaseListener
import com.dareangel.tmessager.interfaces.ILazyLoaderCallback
import com.dareangel.tmessager.interfaces.IPullToLoadMoreListener
import com.dareangel.tmessager.interfaces.MessageListener
import com.dareangel.tmessager.model.MessageData
import com.dareangel.tmessager.`object`.MessengerCodes
import com.dareangel.tmessager.service.MessagingService
import com.dareangel.tmessager.service.comm.IncomingDataFromServiceHandler
import com.dareangel.tmessager.ui.view.MainActivity
import com.dareangel.tmessager.ui.view.messagesdisplayer.MessagesAdapter
import com.dareangel.tmessager.ui.view.messagesdisplayer.MessagesRecyclerViewOverScrollEffect
import com.dareangel.tmessager.ui.view.messagesdisplayer.MessagesRecyclerviewLayoutManager

/**
 * The fragment for the chat room view
 * @param mContext the parent activity context
 * @param mDataManager the data presenter
 */
class ChatRoomFragment(
    private val mContext : MainActivity,
) : Fragment(R.layout.fragment_chatroom), MessageListener, DatabaseListener, ILazyLoaderCallback {

    private var bindView : FragmentChatroomBinding? = null

    private var mMessagesAdapter : MessagesAdapter? = null
    private var mAdapterItemClickListener : MessagesAdapter.ItemListener? = null

    private var mServiceMessenger: Messenger? = null
    private var mChatRoomMessenger = Messenger(IncomingDataFromServiceHandler(this))

    private var bound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {

            // Create a Messenger from the IBinder returned by the service
            mServiceMessenger = Messenger(service)
            bound = true
            // fetch the messages from the database during the initialization
            sendDataToMessagingService(MessengerCodes.FETCH_MSGS)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mServiceMessenger = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(mContext)
        enterTransition = inflater.inflateTransition(R.transition.fade_out)

        // attach the listener to the database
        Database.databaseListener = this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindView = FragmentChatroomBinding.bind(view)

        initListeners()
        init()
    }

    private fun init() {
        // init the recyclerview
        mMessagesAdapter = MessagesAdapter(mContext, mAdapterItemClickListener!!, this)
        bindView!!.msgListRecyclerView.apply {
            adapter = mMessagesAdapter
            layoutManager = MessagesRecyclerviewLayoutManager(mContext)
            edgeEffectFactory = MessagesRecyclerViewOverScrollEffect()
            pullToLoadMoreView = bindView!!.onLoadMoreRoot
        }
    }

    private fun initListeners() {

        // we can listen the keyboard's state by listening to the layout changes of the rootview.
        // adjust the translation of the upper views when the keyboard shows up so the keyboard won't
        // hide the chat messages.
        bindView!!.root.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, _ ->
            val rect = Rect()
            bindView!!.root.getWindowVisibleDisplayFrame(rect)

            bindView!!.scrollView.translationY = -((bottom - rect.bottom).toFloat())
        }
        // Listener for every click on the adpater's item.
        // @param position - The position of the message item on the recyclerview
        // @param status - The status of the message item
        mAdapterItemClickListener = object: MessagesAdapter.ItemListener {
            override fun onItemClick(position: Int) {

            }

            override fun onItemLongClick() {
                // NOT YET SUPPORTED
            }
            // position: its the position at the adapter level
            override fun onResendClick(position: Int, status: String) {

            }
        }

        // Listener for over scrolling on recyclerview
        bindView!!.msgListRecyclerView.pullToLoadMoreListener =
            object : IPullToLoadMoreListener {
                override fun onPulling(pullValue: Float) {
                    bindView!!.lottie.progress = if (pullValue>=1) 1 / pullValue else pullValue
                }

                override fun onRelease() {
                    bindView!!.lottie.playAnimation()
                }

                override fun onClose() {
                    if (bindView!!.lottie.isAnimating)
                        bindView!!.lottie.cancelAnimation()


                }
            }

        // a button's click listener for sending messages
        bindView!!.sendBtn.setOnClickListener {
            val msg = bindView!!.writeMsgEdittxt.text.toString()
            bindView!!.writeMsgEdittxt.setText("")

            if (msg.isNotEmpty()) {
                // send the message to the service
                sendDataToMessagingService(MessengerCodes.SEND_MSG, msg)
            }
        }
    }

    /**
     * Called to send data to the messaging service
     * @param code the code of the message
     * @param data the data to send
     */
    private fun sendDataToMessagingService(code: Int, data: String = "null") {
        if (!bound) return
        val message = Message.obtain(null, code)

        if (data != "null") {
            val bundle = Bundle()
            bundle.putString("data", data)
            message.data = bundle
        }

        try {
            mServiceMessenger?.send(message)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    /**
     * Called to load more messages from the database
     */
    override fun onLoadMoreData() {
        sendDataToMessagingService(MessengerCodes.LOAD_MORE_MSGS)
    }

    /**
     * Fetches the messages from the database
     */
    @SuppressLint("NotifyDataSetChanged")
    override fun onFetchMessages(messages: ArrayList<MessageData>) {
        if (messages.isEmpty()) return

        mMessagesAdapter!!.data = messages
        mMessagesAdapter!!.noMoreData = false
        bindView!!.msgListRecyclerView.adapter?.notifyDataSetChanged()
        bindView!!.msgListRecyclerView.smoothScrollToPosition(mMessagesAdapter?.data?.size!!)

        sendDataToMessagingService(MessengerCodes.FETCH_UNSEEN_MSGS)
    }

    /**
     * Called when the messages are retrieved from load more
     */
    override fun onFetchMessagesFromLoadMore(messages: ArrayList<MessageData>?) {

        if (messages == null) {
            mMessagesAdapter!!.noMoreData = true
            bindView!!.msgListRecyclerView.adapter?.notifyItemChanged(0)
        }

        // add the new messages to the adapter's data at the top of the list but not the duplicated ones
        var i = 0
        messages?.forEach { msg ->
            if (!mMessagesAdapter!!.data.contains(mMessagesAdapter!!.data.find { it.id == msg.id })) {
                mMessagesAdapter!!.data.add(0 + i, msg)

                i++
            }
        }

        bindView!!.msgListRecyclerView.adapter?.notifyItemRangeInserted(0, i)
    }

    /**
     * Called when the message is being sent and updates the UI
     */
    override fun onMessageSending(msg: MessageData) {
        mMessagesAdapter!!.data.add(msg)
        bindView!!.msgListRecyclerView.adapter?.notifyItemInserted(mMessagesAdapter!!.data.size)
        bindView!!.msgListRecyclerView.smoothScrollToPosition(mMessagesAdapter!!.data.size)
    }

    /**
     * Called when the message is sent successfully and updates the UI
     */
    override fun onMessageSent(msg: MessageData) {
        if (mMessagesAdapter!!.data.size == 0) return

        val position = mMessagesAdapter!!.getPositionOfMessageWithId(msg.id)

        if (position == -1) return

        mMessagesAdapter!!.data[position] = msg
        bindView!!.msgListRecyclerView.adapter?.notifyItemChanged(position+1)
        bindView!!.msgListRecyclerView.adapter?.notifyItemChanged(position)
    }

    /**
     * Called when the message is seen and updates the UI
     */
    override fun onMessageSeen(msg: MessageData) {
        if (mMessagesAdapter!!.data.size == 0) return

        val position = mMessagesAdapter!!.getPositionOfMessageWithId(msg.id)
        if (position == -1) return

        mMessagesAdapter!!.data[position] = msg

        bindView!!.msgListRecyclerView.adapter?.notifyItemChanged(position+1)
        bindView!!.msgListRecyclerView.adapter?.notifyItemChanged(position)
    }

    /**
     * Called when the message failed to send and updates the UI
     */
    override fun onMessageFailed() {
        Toast.makeText(mContext, "Message not sent!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Called when a new message is received and updates the UI
     */
    override fun onNewMessage(message: MessageData) {
        val position = mMessagesAdapter!!.getPositionOfMessageWithId(message.id)
        if (position != -1) return

        mMessagesAdapter!!.data.add(message)
        bindView!!.msgListRecyclerView.adapter?.notifyItemInserted(mMessagesAdapter!!.data.size)
        bindView!!.msgListRecyclerView.smoothScrollToPosition(mMessagesAdapter!!.data.size)
    }

    /**
     * Called when other user logged in
     */
    override fun onOtherUserLoggedIn() {
        // change the connection indicator to green
        bindView!!.connectionIndicator.setImageResource(R.drawable.status_online_ind)
    }

    /**
     * Called when other user logged out
     */
    override fun onOtherUserLoggedOut() {
        // change the connection indicator to red
        bindView!!.connectionIndicator.setImageResource(R.drawable.status_offline_ind)
    }

    override fun onStart() {
        super.onStart()
        // start the foreground service
        val intentService = Intent(mContext, MessagingService::class.java)
        intentService.putExtra("messenger", mChatRoomMessenger)
        ContextCompat.startForegroundService(mContext, intentService)

        // Bind to the service
        val intent = Intent(mContext, MessagingService::class.java)
        mContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        Database.initialize(mContext)
        Database.online()
    }

    override fun onStop() {
        super.onStop()
        // Unbind from the service
        mContext.unbindService(serviceConnection)

        sendDataToMessagingService(MessengerCodes.REMOVE_MESSENGER_CLIENT)
        Database.offline()
    }

    override fun onDestroy() {
        bindView = null
        super.onDestroy()
    }
}