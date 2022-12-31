package com.dareangel.tmessager.data.model.interfaces

interface IPullToLoadMoreListener {

    fun onPulling(pullValue: Float)
    fun onRelease()
    fun onClose()
}