package com.dareangel.tmessager.ui.view.messagesdisplayer

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

class MessagesRecyclerviewLayoutManager(
    context: Context
) : LinearLayoutManager(context) {

    private val TAG = "MessagesRecyclerviewLay"

    private var isScrollable = true

    fun canScroll(bool: Boolean) {
        isScrollable = bool

    }

    override fun canScrollVertically(): Boolean {
        return isScrollable && super.canScrollVertically()
    }
}