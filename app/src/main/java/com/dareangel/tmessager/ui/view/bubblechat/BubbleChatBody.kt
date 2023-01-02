package com.dareangel.tmessager.ui.view.bubblechat

import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.view.updateLayoutParams
import com.dareangel.tmessager.R
import com.dareangel.tmessager.data.model.Message
import com.dareangel.tmessager.data.model.interfaces.BubbleChat
import com.dareangel.tmessager.data.model.interfaces.IPullToLoadMoreListener
import com.dareangel.tmessager.data.model.interfaces.IServerListener
import com.dareangel.tmessager.data.model.interfaces.UnseenMessagesListener
import com.dareangel.tmessager.manager.DataManager
import com.dareangel.tmessager.ui.animator.Animator
import com.dareangel.tmessager.ui.sound.SoundPlayer
import com.dareangel.tmessager.ui.view.ChatHandler
import com.dareangel.tmessager.ui.view.layouts.BubbleChatView
import com.dareangel.tmessager.ui.view.messagesdisplayer.MessagesAdapter
import com.dareangel.tmessager.ui.view.messagesdisplayer.MessagesRecyclerViewOverScrollEffect
import com.dareangel.tmessager.ui.view.messagesdisplayer.MessagesRecyclerview2
import com.dareangel.tmessager.ui.view.messagesdisplayer.MessagesRecyclerviewLayoutManager
import io.github.florent37.shapeofview.shapes.CircleView
import kotlinx.coroutines.*

@Suppress("FunctionName")
@RequiresApi(Build.VERSION_CODES.O)
class BubbleChatBody(
    private val mContext: Context,
    mDeviceScreen: Size,
    private val mDataManager: DataManager
) : BubbleChat(), IServerListener {

    private val mChatHandler = ChatHandler(mContext)
    private val mSoundPlayer = SoundPlayer(mContext)
    private val mCoroutineScope = CoroutineScope(Job() + Dispatchers.Default)

    private var mIsOpen = false
    val isOpen: Boolean
        get() = mIsOpen

    private val mHeight = .85 * mDeviceScreen.height
    val height: Double
        get() = mHeight

    private var mMessagesSize = -1
    private var messagesSize: Int
        get() = mMessagesSize
        set(value) {mMessagesSize=value}

    private var mUnseenMessagesListener: UnseenMessagesListener? = null
    var unseenMessagesListener: UnseenMessagesListener?
        get() = mUnseenMessagesListener
        set(value) {mUnseenMessagesListener=value}

    private val mInflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    override val inflater: LayoutInflater
        get() = mInflater

    @SuppressLint("InflateParams")
    private var mRootView = mInflater.inflate(R.layout.layout_chatbubble_body, null)
    override val rootView: View
        get() = mRootView

    override val layoutParams: WindowManager.LayoutParams
        get() = mParam

    // views
    private var mParentRootView: BubbleChatView = mRootView.findViewById(R.id.root)
    private var mChatRootView: View = mRootView.findViewById(R.id.chatRoot)
    private var mMsgRecyclerview: MessagesRecyclerview2 = mRootView.findViewById(R.id.msgRecyclerview)
    private var mLoadMoreView: ImageView = mRootView.findViewById(R.id.loadmore)

    private val mParam = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_DIM_BEHIND or
        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT
    )

    private var mMessagesAdapter : MessagesAdapter? = null
    private var mAdapterItemClickListener : MessagesAdapter.ItemListener? = null

    init {
        mParam.dimAmount = 0.5f
        mChatRootView.updateLayoutParams {
            height = mHeight.toInt()
        }
        _initListener()
        initialize()
    }

    override fun initialize() {
        mChatRootView.translationY = mHeight.toFloat()
        mParentRootView.visibility = View.GONE

        mChatHandler.apply {
            dataManager = mDataManager
            messagesAdapter = mMessagesAdapter
            adapterItemListener = mAdapterItemClickListener
            connStatusView = mRootView.findViewById(R.id.connection_indicator)
            msgRecyclerview = mMsgRecyclerview
        }

        // initialize socket listener
        mDataManager.initSocket(mContext, this)
    }

    private fun _initListener() {
        mParentRootView.setOnBackPressedListener = {
            // on back pressed
            if (isOpen)
                hide()
        }

        mAdapterItemClickListener = object: MessagesAdapter.ItemListener {
            override fun onItemClick(position: Int) {
                mChatHandler.messagesAdapter?.showSendStatus(position)
            }

            override fun onItemLongClick() {
                // NOT YET SUPPORTED
            }

            override fun onResendClick(position: Int, status: String) {
                if (status != Message.NOT_SENT || !mDataManager.socket!!.connected)
                    return

                val resendMsg = mChatHandler.messagesAdapter!!.getLoadedMessages()[position-1]
                mDataManager.socket?.sendMessage(
                    DataManager.TSocket.SERVICE,
                    resendMsg.msg!!,
                    position-1,
                    Message.SENDING,
                    Message.Companion.Type.RESEND
                )
            }

        }

        // Listener for over scrolling on recyclerview
        mMsgRecyclerview.pullToLoadMoreListener =
            object : IPullToLoadMoreListener {
                override fun onPulling(pullValue: Float) {
                    val ratio = if (pullValue>=1) 1 / pullValue else pullValue
                    mLoadMoreView.rotation = -(ratio * 360)
                }

                override fun onRelease() {}

                override fun onClose() {}
            }

        // a button's click listener for sending messages
        mRootView.findViewById<CircleView>(R.id.sendBtn).setOnClickListener {
            val sendEdittxt = mRootView.findViewById<EditText>(R.id.sendEdittxt)
            val msg = sendEdittxt.text.toString()
            sendEdittxt.setText("")

            if (mDataManager.socket!!.connected) {
                // send the message to the client's chat mate
                mDataManager.socket?.sendMessage(
                    DataManager.TSocket.SERVICE,
                    msg,
                    mChatHandler.messagesAdapter!!.itemCount-1,
                    Message.SENDING
                )
            } else {
                mDataManager.socket?.sendMessage(
                    DataManager.TSocket.SERVICE,
                    msg,
                    mChatHandler.messagesAdapter!!.itemCount-1,
                    Message.NOT_SENT
                )
            }
        }
    }

    override fun update() {
        mChatHandler.fetchMessages {
            // on messages fetched
            mChatHandler.msgRecyclerview?.apply {
                adapter = mChatHandler.messagesAdapter
                scrollToPosition(mChatHandler.messagesAdapter!!.itemCount-1)
            }

        }
    }

    override fun show() {
        // cancel timer for destroying the service
        mDataManager.socket?.unscheduleServiceDestroy()
        mIsOpen = true
        mParentRootView.visibility = View.VISIBLE

        Animator.animate(
            mChatRootView, "translationY", 0f, 500
        ).addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                seenMessage()
            }
        })
    }

    override fun hide() {
        mDataManager.socket?.scheduleServiceDestroy()
        mIsOpen = false

        Animator.animate(
            mChatRootView, "translationY", mHeight.toFloat(), 500
        ).addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                mParentRootView.visibility = View.GONE
            }
        })
    }

    /**
     * This is called after this client notify the socket service that we want to send a message
     * to the other peer so we can update the recyclerview of the new message.
     */
    override fun sendMessage(it: HashMap<String, Any>) {
        // if the type of the message is for resending only then we should not add the message
        if ((it["type"] as String) == Message.Companion.Type.RESEND.toString())
            return

        val invoker = (it["from"] as Double).toInt()
        var addToDB = true
        // if the invoker is coming from the UI , the message is already added in the database
        // so, we don't need to add the message again to the database.
        if (invoker == DataManager.TSocket.UI)
            addToDB = false

        mChatHandler.addMessage(ChatHandler.USER, it["msg"] as String, it["status"] as String, addToDB)
    }

    override fun onConnect(isFirstInit: Boolean) {
        mChatHandler.initialize {
            // on message fetch
            mCoroutineScope.launch(Dispatchers.Main) {
                mChatHandler.msgRecyclerview?.apply {
                    layoutManager = MessagesRecyclerviewLayoutManager(mContext)
                    adapter = mChatHandler.messagesAdapter
                    pullToLoadMoreView = mRootView.findViewById(R.id.onLoadMoreRoot)
                    edgeEffectFactory = MessagesRecyclerViewOverScrollEffect().also {
                        it.loadMoreOffsetRatio = 0.9f
                    }
                    setHasFixedSize(true)

                    scrollToPosition(mChatHandler.messagesAdapter!!.itemCount-1)
                }

                mMessagesAdapter = mChatHandler.messagesAdapter
                messagesSize = mMessagesAdapter!!.itemCount
            }
        }
    }

    override fun onDisconnect(it: Array<Any>) {
        mChatHandler.onDisconnect()
    }

    override fun onConnectError() {
        mChatHandler.onConnectError()
    }

    override fun onNewMessage(it: Array<Any>) {
        mChatHandler.onNewMessage(it, isOpen) { count ->
            // call new unseen message if there's any
            mSoundPlayer.playNotificationSound()
            mUnseenMessagesListener!!.onNewUnseenMessage(count)
        }
    }

    override fun seenMessage() {
        mChatHandler.messageSeen()
    }

    override fun onMessageSent(it: Array<Any>) {
        mChatHandler.onMessageSent(it)
    }

    override fun onMessageReceived(it: Array<Any>) {
        mChatHandler.onMessageReceived(it)
    }

    override fun onUserJoined(it: Array<Any>) {
        mChatHandler.onUserJoined(it)
    }

    override fun onEnteredRoom(it: Array<Any>) {
        mChatHandler.onEnteredRoom(it)
    }

    override fun closeApplication() {

    }

    override fun onTyping(it: Array<Any>) {
        TODO("Not yet implemented")
    }

    override fun onStopTyping(it: Array<Any>) {
        TODO("Not yet implemented")
    }
}