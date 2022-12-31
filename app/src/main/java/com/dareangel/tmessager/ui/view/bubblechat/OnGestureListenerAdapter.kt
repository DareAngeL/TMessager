package com.dareangel.tmessager.ui.view.bubblechat

import android.view.GestureDetector.OnGestureListener
import android.view.MotionEvent

abstract class OnGestureListenerAdapter : OnGestureListener {

    override fun onDown(e: MotionEvent): Boolean { return true }
    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return true
    }

    override fun onLongPress(e: MotionEvent) {}
    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return true
    }

    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean { return true }
}