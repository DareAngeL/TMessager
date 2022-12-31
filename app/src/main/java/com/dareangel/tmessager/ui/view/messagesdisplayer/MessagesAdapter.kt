package com.dareangel.tmessager.ui.view.messagesdisplayer

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dareangel.tmessager.R
import com.dareangel.tmessager.data.model.Message
import com.dareangel.tmessager.data.model.Message.Companion.LAST_MSG
import com.dareangel.tmessager.data.model.Message.Companion.MSG_INSERTED
import com.dareangel.tmessager.data.model.Message.Companion.SECOND_LAST_MSG
import com.dareangel.tmessager.data.model.interfaces.ILazyLoaderCallback
import com.dareangel.tmessager.manager.DataManager
import com.google.android.material.textview.MaterialTextView
import io.github.florent37.shapeofview.shapes.CircleView
import kotlinx.coroutines.*

class MessagesAdapter(
    private val mContext: Context,
    private val mUser: String,
    data: ArrayList<Message>,
    private val onItemClickListener: ItemListener
) : RecyclerView.Adapter<MessagesAdapter.MessagesViewHolder>(), ILazyLoaderCallback {

    private val mCoroutineScope = CoroutineScope(Job() + Dispatchers.Default)
    private var mLoadedData : ArrayList<Message> = ArrayList()
    private var mRawData = mutableListOf<Message>().apply {
        addAll(data)
    }

    private val MAX_MSG_TO_LOAD = 20
    // use to determine of how many extra items in the recyclerview's data, other than the messages data.
    private val EXTRA_ITEMS = 1

    // #region: property access
    val rawDataSize : Int
        get() = mRawData.size

    val rawData : MutableList<Message>
        get() = mRawData
    // #endregion

    interface ItemListener {
        fun onItemClick(position: Int)
        fun onItemLongClick()
        fun onResendClick(position: Int, status: String)
    }

    enum class PositionType {
        ADAPTER_POSITION,
        DATA_POSITION
    }

    init {
        if (data.isNotEmpty()) {
            if (data.size <= 20) {
                mLoadedData.addAll(data)
            } else {
                mLoadedData = data.takeLast(20) as ArrayList
            }
        }
    }

    companion object {
        val LOAD_MORE = 2
    }

    private var SHOW_SEND_STATUS_POS = -1
    fun showSendStatus(pos: Int) {
        SHOW_SEND_STATUS_POS = pos
        notifyItemChanged(pos)
    }

    fun appendMessage(dataPresenter: DataManager, msg: Message, isAddToDB: Boolean) {
        if (mLoadedData.contains(msg))
            return

        mLoadedData.add(msg)
        mRawData.add(msg)
        // also adds to the local database
        if (isAddToDB)
            dataPresenter.msgsDBTable.addMessage(msg)

        notifyChanges(MSG_INSERTED)
        notifyChanges(SECOND_LAST_MSG)
    }

    fun getLoadedMessages() : ArrayList<Message> {
        return mLoadedData
    }

    /**
     * Update the message status at the specified position
     * @param pos The position of the message at the adapter level
     */
    fun updateMessageStatus(pos: Int, status: String) : Message {
        val posDataLevel = pos-1
        lateinit var msg: Message

        getLoadedMessages().apply {
            msg = Message(
                this[posDataLevel].msg,
                this[posDataLevel].sender,
                this[posDataLevel].pos,
                status
            ).apply {
                val m = this
                rawData.apply {
                    this[posDataLevel] = m
                }
            }
            this[posDataLevel] = msg
        }

        notifyItemChanged(pos)
        return msg
    }

    fun lazyLoadCallback() : ILazyLoaderCallback {
        return this
    }

    fun isDataFullyLoaded() : Boolean {
        return itemCount-EXTRA_ITEMS == mRawData.size
    }

    fun getPosition(pos: Int, posType: PositionType) : Int {
        return if (posType == PositionType.DATA_POSITION) {
            pos-1
        } else {
            pos
        }
    }

    /**
     * Notifies adapter about the changes on its last and second to the last data and update it.
     * @param what Determine what item should be updated on the recyclerview.
     *        Either LAST_MSG/SECOND_LAST_MSG/MSG_INSERTED
     */
    fun notifyChanges(what: Int) {
        when (what) {
            LAST_MSG -> {
                notifyItemChanged(itemCount - 1)
            }
            SECOND_LAST_MSG -> {
                notifyItemChanged(itemCount-2)
            }
            else -> {
                notifyItemInserted(itemCount - 1)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessagesViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                when (viewType) {
                    Message.USER -> R.layout.recycler_user_fragment
                    Message.CHAT_MATE -> R.layout.recycler_chatmate_fragment
                    else -> R.layout.recycler_loadmore_txt_fragment
                }
                , parent,
                false
            )

        return MessagesViewHolder(parent.context, view, viewType, onItemClickListener)
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 -> {
                LOAD_MORE
            }
            mUser == mLoadedData[position-1].sender -> {
                Message.USER
            }
            else -> {
                Message.CHAT_MATE
            }
        }
    }

    override fun onBindViewHolder(holder: MessagesViewHolder, position: Int) {
        // the position at 0 is always the {load_more_txt} layout
        if (position == 0) {
            _updateLoadToMoreTxt(holder)
            return
        }

        holder.bind(
            mLoadedData[position-1],
            position,
            SHOW_SEND_STATUS_POS == position,
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

        SHOW_SEND_STATUS_POS = -1
    }

    override fun getItemCount(): Int {
        return mLoadedData.size + EXTRA_ITEMS
    }

    /**
     * Called when the user wants to load more messages data by pulling down the recyclerview
     */
    override fun onLoadMoreData() {
        // * see how many are left if 20 + the total loaded data were taken from {data},
        // * if the total item left is bigger than or equal to zero then take the 20 items
        // * else if the total item left is less than zero then let's just take the items left.
        val count = mRawData.size - (mLoadedData.size + MAX_MSG_TO_LOAD)
        if (count >= 0) {
            mLoadedData = (mRawData.takeLast(mLoadedData.size+MAX_MSG_TO_LOAD).filter {
                !mLoadedData.contains(it)
            } + mLoadedData.take(mLoadedData.size)) as ArrayList<Message>

            notifyItemRangeInserted(0, MAX_MSG_TO_LOAD)
        } else {
            val dat = mRawData.take(count+MAX_MSG_TO_LOAD) + mLoadedData.take(mLoadedData.size)
            if (dat.isNotEmpty()) {
                mLoadedData = dat as ArrayList<Message>
                notifyItemRangeInserted(0, count+MAX_MSG_TO_LOAD)
            }
        }
    }

    // # region: private functions

    private fun _updateLoadToMoreTxt(holder: MessagesViewHolder) {
        val view = holder.itemView
        val text = view.findViewById<MaterialTextView>(R.id.text)
        val arrow = view.findViewById<ImageView>(R.id.arrow)
        // if the data is fully loaded then change the text
        // {load_more_txt} to {no_more_msg_txt}. Otherwise, don't change it.
        if (isDataFullyLoaded()) {
            // Just hide the load more text when
            // there are only less than 20 text messages.
            // Otherwise, don't hide it.
            if (mRawData.size <= 20) {
                text.text = mContext.getString(R.string.new_msgs_txt)
                arrow.visibility = View.GONE
                return
            } else if (mRawData.isEmpty()) {
                text.text = mContext.getString(R.string.no_msgs_txt)
                arrow.visibility = View.GONE
                return
            }

            text.text = mContext.getString(R.string.no_more_msg_txt)
            arrow.visibility = View.GONE
        } else {
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
        private var mNotSentTextRootColorInt = ContextCompat.getColor(mContext, android.R.color.darker_gray)

        private var mItemPosition = -1
        private var mItemStatus = ""

        private val TAG = "MessagesAdapter"

        init {
            mText = itemView.findViewById(R.id.text)
            mParent = itemView.findViewById(R.id.parent)

            if (viewType != Message.CHAT_MATE) {
                mSendStatus = itemView.findViewById(R.id.sendStatus)
                mTextRoot = itemView.findViewById(R.id.textRoot)
                mResendBtn = itemView.findViewById(R.id.resend)

                mTextRoot?.setOnClickListener {
                    onItemClicked.onItemClick(mItemPosition)
                }

                mTextRoot?.setOnLongClickListener {
                    onItemClicked.onItemLongClick()
                    return@setOnLongClickListener true
                }

                mResendBtn?.setOnClickListener {
                    onItemClicked.onResendClick(mItemPosition, mItemStatus)
                }
            } else {
                mChatmateTxt = itemView.findViewById(R.id.babeTxt)
            }
        }

        fun bind(
            message: Message,
            pos: Int,
            isShowSendStatus: Boolean,
            isLastPos: Boolean
        ) {
            mItemPosition = pos
            mItemStatus = message.status!!

            when (message.status) {
                Message.SENDING -> {
                    mSendStatus?.visibility = View.VISIBLE
                    mResendBtn?.visibility = View.GONE
                    mTextRoot?.background?.setTint(mDefaultTextRootColorInt)
                    mSendStatus?.text = Message.SENDING
                }
                Message.NOT_SENT -> {
                    mSendStatus?.visibility = View.VISIBLE
                    mResendBtn?.visibility = View.VISIBLE
                    mTextRoot?.background?.setTint(mNotSentTextRootColorInt)
                    mSendStatus?.text = Message.NOT_SENT
                }
                Message.SEEN -> {
                    mTextRoot?.background?.setTint(mDefaultTextRootColorInt)
                    mResendBtn?.visibility = View.GONE
                    mSendStatus?.text = Message.SEEN
                    if (!isLastPos)
                        mSendStatus?.visibility = View.GONE
                    else
                        mSendStatus?.visibility = View.VISIBLE
                }
                else -> {
                    mSendStatus?.text = Message.SENT
                    mResendBtn?.visibility = View.GONE
                    mTextRoot?.background?.setTint(mDefaultTextRootColorInt)
                    if (isLastPos)
                        mSendStatus?.visibility = View.VISIBLE
                    else if (isShowSendStatus)
                        mSendStatus?.visibility = View.VISIBLE
                    else
                        mSendStatus?.visibility = View.GONE
                }
            }

            mChatmateTxt?.text = "Babe"
            mText?.text = message.msg
        }

        fun parent() : RelativeLayout? {
            return mParent
        }

        fun text() : TextView? {
            return mText
        }
    }
}