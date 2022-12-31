package com.dareangel.tmessager.ui.view.fragments

import android.animation.AnimatorListenerAdapter
import android.graphics.Rect
import android.os.*
import android.view.View
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.dareangel.tmessager.R
import com.dareangel.tmessager.data.model.Message
import com.dareangel.tmessager.data.model.interfaces.IPullToLoadMoreListener
import com.dareangel.tmessager.data.model.interfaces.IServerListener
import com.dareangel.tmessager.databinding.FragmentChatroomBinding
import com.dareangel.tmessager.manager.DataManager
import com.dareangel.tmessager.ui.animator.Animator
import com.dareangel.tmessager.ui.view.ChatHandler
import com.dareangel.tmessager.ui.view.MainActivity
import com.dareangel.tmessager.ui.view.messagesdisplayer.MessagesAdapter
import com.dareangel.tmessager.ui.view.messagesdisplayer.MessagesRecyclerViewOverScrollEffect
import com.dareangel.tmessager.ui.view.messagesdisplayer.MessagesRecyclerviewLayoutManager
import org.json.JSONObject

/**
 * The fragment for the chat room view
 * @param mContext the parent activity context
 * @param mUser the user's username
 * @param isAddUser determine if the username should be inserted to the database or not.
 */
class ChatRoomFragment(
    private val mContext : MainActivity,
    private val mDataManager: DataManager,
) : Fragment(R.layout.fragment_chatroom), IServerListener {

    private var bindView : FragmentChatroomBinding? = null

    private var mMessagesAdapter : MessagesAdapter? = null
    private var mAdapterItemClickListener : MessagesAdapter.ItemListener? = null

    private val mChatHandler = ChatHandler(mContext)
    private var mForceCloseActivityCallback: () -> Unit = {}
    var forceCloseActivityCallback: () -> Unit
        get() = mForceCloseActivityCallback
        set(value) {mForceCloseActivityCallback=value}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(mContext)
        enterTransition = inflater.inflateTransition(R.transition.fade_out)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindView = FragmentChatroomBinding.bind(view)

        _initListeners()
        mChatHandler.apply {
            dataManager = mDataManager
            messagesAdapter = mMessagesAdapter
            adapterItemListener = mAdapterItemClickListener
            connStatusView = bindView?.connectionIndicator
            msgRecyclerview = bindView?.msgListRecyclerView
        }

        // initialize socket listener and connect to the server
        mDataManager.initSocket(mContext, this)
    }

    /**
     * Called when the client is successfully connected to the server
     */
    private fun _init() {
        mChatHandler.initialize {
            //onMessageFetch
            _hideConnectingView()
        }
    }

    private fun _initListeners() {
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
                mChatHandler.messagesAdapter?.showSendStatus(position)
            }

            override fun onItemLongClick() {
                // NOT YET SUPPORTED
            }
            // position: its the position at the adapter level
            override fun onResendClick(position: Int, status: String) {
                if (status != Message.NOT_SENT || !mDataManager.socket!!.connected)
                    return

                val resendMsg = mChatHandler.messagesAdapter!!.getLoadedMessages()[position-1]
                mDataManager.socket?.sendMessage(
                    DataManager.TSocket.UI,
                    resendMsg.msg!!,
                    position-1,
                    Message.SENDING,
                    Message.Companion.Type.RESEND
                )
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

            if (mDataManager.socket!!.connected) {
                // notify the socket service that we want to send a message to the other peer
                mDataManager.socket?.sendMessage(
                    DataManager.TSocket.UI,
                    msg,
                    mChatHandler.messagesAdapter!!.itemCount-1,
                    Message.SENDING
                )
            } else {
                mDataManager.socket?.sendMessage(
                    DataManager.TSocket.UI,
                    msg,
                    mChatHandler.messagesAdapter!!.itemCount-1,
                    Message.NOT_SENT
                )
            }
        }
    }

    /**
     * Hides the connecting view
     */
    private fun _hideConnectingView() {
        if (bindView?.connectingRoot == null)
            return

        Animator.animate(bindView!!.connectingRoot, "alpha", 0f, 800)
            .addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    super.onAnimationEnd(animation)
                    if (bindView == null)
                        return

                    // remove the connectingRoot view
                    bindView!!.connectingRoot.visibility = View.GONE
                    bindView!!.connectingLottie.cancelAnimation()

                    mChatHandler.msgRecyclerview?.apply {
                        layoutManager = MessagesRecyclerviewLayoutManager(mContext)
                        adapter = mChatHandler.messagesAdapter
                        pullToLoadMoreView = bindView!!.onLoadMoreRoot
                        edgeEffectFactory = MessagesRecyclerViewOverScrollEffect()
                        setHasFixedSize(true)

                        scrollToPosition(mChatHandler.messagesAdapter!!.itemCount-1)
                    }

                    mMessagesAdapter = mChatHandler.messagesAdapter
                }
            })
    }

    override fun onDestroy() {
        bindView = null
        super.onDestroy()
    }

    /**
     * This is called after this client notify the socket service that we want to send a message
     * to the other peer so we can update the recyclerview of the new message.
     */
    override fun sendMessage(it: HashMap<String, Any>) {
        // if the type of the message is for resending only then we should not add the message
        if ((it["type"] as String) == Message.Companion.Type.RESEND.toString())
            return

        mChatHandler.addMessage(
            ChatHandler.USER,
            it["msg"] as String,
            it["status"] as String,
            true
        )
    }

    override fun onConnect() {
        _init()
    }

    /**
     * Called when client's chat mate disconnected
     */
    override fun onDisconnect(it: Array<Any>) {
        mChatHandler.onDisconnect()
    }

    override fun onConnectError() {
        mChatHandler.onConnectError()
    }

    /**
     * Called whenever there's a new message from the chat mate
     */
    override fun onNewMessage(it: Array<Any>) {
        mChatHandler.onNewMessage(it)
    }

    override fun seenMessage() {
        mChatHandler.messageSeen()
    }

    /**
     * Called whenever the sent message from the client was received by the other peer.
     */
    override fun onMessageReceived(it: Array<Any>) {
        mChatHandler.onMessageReceived(it)
    }

    /**
     * Called when the message was sent to the server
     */
    override fun onMessageSent(it: Array<Any>) {
        mChatHandler.onMessageSent(it)
    }

    /**
     * Called when the client successfully entered the chat room
     */
    override fun onEnteredRoom(it: Array<Any>) {
        mChatHandler.onEnteredRoom(it)
    }

    /**
     * Called when the chat mate joined the room
     */
    override fun onUserJoined(it: Array<Any>) {
        mChatHandler.onUserJoined(it)
    }

    /**
     * Called to close the application
     */
    override fun closeApplication() {
        forceCloseActivityCallback.invoke()
    }

    override fun onTyping(it: Array<Any>) {
        TODO("Not yet implemented")
    }

    override fun onStopTyping(it: Array<Any>) {
        TODO("Not yet implemented")
    }
}