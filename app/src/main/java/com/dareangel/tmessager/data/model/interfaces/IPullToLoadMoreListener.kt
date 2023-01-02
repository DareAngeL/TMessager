package com.dareangel.tmessager.data.model.interfaces

interface IPullToLoadMoreListener {

    /**
     * When the user pull the recyclerview
     * @param pullValue the pull value
     */
    fun onPulling(pullValue: Float)

    /**
     * When the user stops pulling the recyclerview
     */
    fun onRelease()

    /**
     * When the recyclerview finally stops pulling animation
     */
    fun onClose()
}