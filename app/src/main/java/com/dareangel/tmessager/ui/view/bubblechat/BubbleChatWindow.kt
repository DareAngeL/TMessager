package com.dareangel.tmessager.ui.view.bubblechat

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.Rect
import android.os.Build
import android.util.Size
import android.view.*
import androidx.annotation.RequiresApi
import com.dareangel.tmessager.manager.DataManager
import com.dareangel.tmessager.util.Utility

@RequiresApi(Build.VERSION_CODES.O)
class BubbleChatWindow(
    private val mContext: Context
) {
    private val TAG = "BubbleChatWindow"

    private val mDataManager = DataManager(this.javaClass.name).apply {
        msgsDBTable.open(mContext)
    }

    private var mWinManager: WindowManager = mContext.getSystemService(WINDOW_SERVICE) as WindowManager
    private var mDeviceScreen: Size = Utility.deviceScreen(mWinManager)
    private var mDeviceScreenSpace: Rect = Rect(
        -mDeviceScreen.width/2, -mDeviceScreen.height/2,
        mDeviceScreen.width/2, mDeviceScreen.height/2
    )

    private val mTrashBin = BubbleChatTrashBin(mContext, mDeviceScreen)
    private val mChatBody = BubbleChatBody(mContext, mDeviceScreen, mDataManager)
    private val mChatHead = BubbleChatHead(mContext, mWinManager, mDeviceScreenSpace, mTrashBin, mChatBody)

    fun unbind() {
        mDataManager.msgsDBTable.close(true)
        mDataManager.socket?.unBind()
    }

    fun bind() {
        mDataManager.msgsDBTable.open(mContext)
        mDataManager.socket?.bind()
    }

    /**
     * Open the window
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun open() {
        if (mChatHead.rootView.windowToken == null && mChatHead.rootView.parent == null) {
            mWinManager.addView(mChatBody.rootView, mChatBody.layoutParams)
            mWinManager.addView(mChatHead.rootView, mChatHead.layoutParams)
            mWinManager.addView(mTrashBin.rootView, mTrashBin.layoutParams)

            // update or fetch the messages everytime the view added to the window
            mChatBody.update()
        }
    }

    /**
     * Close the window
     */
    fun close() {
        try {
            mWinManager.removeView(mChatHead.rootView)
            mWinManager.removeView(mTrashBin.rootView)
            mWinManager.removeView(mChatBody.rootView)
            mChatHead.update()
            mTrashBin.update()
            mChatBody.update()
        } catch (_: Exception) {}
    }
}