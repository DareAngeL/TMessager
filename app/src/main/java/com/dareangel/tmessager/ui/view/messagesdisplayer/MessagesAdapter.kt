package com.dareangel.tmessager.ui.view.messagesdisplayer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dareangel.tmessager.R
import com.dareangel.tmessager.interfaces.ILazyLoaderCallback
import com.dareangel.tmessager.model.Message
import com.dareangel.tmessager.model.MessageData
import com.dareangel.tmessager.ui.view.fragments.ChatRoomFragment
import com.google.android.material.textview.MaterialTextView
import io.github.florent37.shapeofview.shapes.CircleView
import kotlinx.coroutines.*

class MessagesAdapter(
    private val mContext: Context,
    private val onItemClickListener: ItemListener,
    private val lazyLoaderListener: ILazyLoaderCallback
) : RecyclerView.Adapter<MessagesAdapter.MessagesViewHolder>(), ILazyLoaderCallback {

    private val mData: ArrayList<MessageData> = ArrayList()
    private var mUser = Message.USER
    private var isNoMoreData = false // no more data from the database

    companion object {
        const val USER = 1
        const val CHAT_MATE = 2
    }

    var data : ArrayList<MessageData>
        get() = mData
        set(value) {
            mData.clear()
            mData.addAll(value)
        }

    var noMoreData : Boolean
        get() = isNoMoreData
        set(value) {
            isNoMoreData = value
        }

    interface ItemListener {
        fun onItemClick(position: Int)
        fun onItemLongClick()
        fun onResendClick(position: Int, status: String)
    }

    fun lazyLoadCallback() : ILazyLoaderCallback {
        return this
    }

    fun getPositionOfMessageWithId(id: String) : Int {
        // get the position of the message with the given id
        return mData.indexOf(mData.find { it.id == id })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessagesViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                when (viewType) {
                    USER -> R.layout.recycler_user_fragment
                    CHAT_MATE -> R.layout.recycler_chatmate_fragment
                    else -> R.layout.recycler_loadmore_txt_fragment
                }
                , parent,
                false
            )

        return MessagesViewHolder(parent.context, view, viewType, onItemClickListener)
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 -> 0
            mUser == mData[position-1].sender -> USER
            else -> CHAT_MATE
        }
    }

    override fun onBindViewHolder(holder: MessagesViewHolder, position: Int) {
        // the position at 0 is always the {load_more_txt} layout
        if (position == 0) {
            updateLoadToMoreTxt(holder)
            return
        }

        holder.bind(
            mData[position-1],
            position,
            itemCount-1 == position
        )

        // add padding on the last item view
        if (position == itemCount-1) {
            holder.parent()!!.setPadding(
                holder.parent()!!.paddingStart,
                holder.parent()!!.paddingTop,
                holder.parent()!!.paddingRight,
                15
            )
        }
    }

    override fun getItemCount(): Int {
        return mData.size + 1
    }

    /**
     * Called when the user wants to load more messages data by pulling down the recyclerview
     */
    override fun onLoadMoreData() {
        lazyLoaderListener.onLoadMoreData()
    }

    // # region: private functions
    private fun updateLoadToMoreTxt(holder: MessagesViewHolder) {
        val view = holder.itemView
        val text = view.findViewById<MaterialTextView>(R.id.text)
        val arrow = view.findViewById<ImageView>(R.id.arrow)

        if (isNoMoreData || mData.size < Message.MAX_MSG_TO_LOAD) {

            if (!isNoMoreData) {
                isNoMoreData = true
            }

            text.text = mContext.getString(R.string.no_more_msg_txt)
            arrow.visibility = View.GONE

            return
        }

        if (mData.isEmpty()) {
            text.text = mContext.getString(R.string.no_msgs_txt)
            arrow.visibility = View.GONE
        } else if (mData.size >= Message.MAX_MSG_TO_LOAD) {
            text.text = mContext.getString(R.string.load_more_txt)
            arrow.visibility = View.VISIBLE
        }
    }

    // # end region

    class MessagesViewHolder(
        mContext: Context,
        itemView: View,
        viewType: Int,
        onItemClicked: ItemListener
    ) : RecyclerView.ViewHolder(itemView) {
        private var mText : TextView? = null
        private var mParent : RelativeLayout? = null
        private var mSendStatus : TextView? = null
        private var mTextRoot : FrameLayout? = null
        private var mChatmateTxt : TextView? = null
        private var mResendBtn : CircleView? = null

        private var mDefaultTextRootColorInt = ContextCompat.getColor(mContext, R.color.pinkDark)

        private val TAG = "MessagesAdapter"

        init {
            mText = itemView.findViewById(R.id.text)
            mParent = itemView.findViewById(R.id.parent)
            mChatmateTxt = itemView.findViewById(R.id.chatMate)

            if (viewType == USER) {
                mSendStatus = itemView.findViewById(R.id.sendStatus)
                mTextRoot = itemView.findViewById(R.id.textRoot)
                mResendBtn = itemView.findViewById(R.id.resend)
            }
        }

        fun bind(
            message: MessageData,
            pos: Int,
            isLastPos: Boolean
        ) {

            mResendBtn?.visibility = View.GONE

            mTextRoot?.setOnClickListener {
                if (mSendStatus?.visibility == View.VISIBLE) {
                    mSendStatus?.visibility = View.GONE
                } else {
                    mSendStatus?.visibility = View.VISIBLE
                }
            }

            when {
                message.seenBy.isNotEmpty() -> {
                    mTextRoot?.background?.setTint(mDefaultTextRootColorInt)
                    mSendStatus?.text = "Seen"
                    if (!isLastPos)
                        mSendStatus?.visibility = View.GONE
                    else
                        mSendStatus?.visibility = View.VISIBLE
                }

                message.status == MessageData.STATUS_SENDING -> {
                    mSendStatus?.visibility = View.VISIBLE
                    mTextRoot?.background?.setTint(mDefaultTextRootColorInt)
                    mSendStatus?.text = "Sending..."
                }

                message.status == MessageData.STATUS_SENT -> {
                    mSendStatus?.text = "Sent"
                    mTextRoot?.background?.setTint(mDefaultTextRootColorInt)
                    if (isLastPos)
                        mSendStatus?.visibility = View.VISIBLE
                    else {
                        if (message.status == MessageData.STATUS_SENT)
                            mSendStatus?.visibility = View.GONE
                        else
                            mSendStatus?.visibility = View.VISIBLE
                    }
                }
            }

            mChatmateTxt?.text = "Babe"
            mText?.text = message.message
        }

        fun parent() : RelativeLayout? {
            return mParent
        }

        fun text() : TextView? {
            return mText
        }
    }
}