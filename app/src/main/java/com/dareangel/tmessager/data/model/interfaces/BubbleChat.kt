package com.dareangel.tmessager.data.model.interfaces

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.Interpolator
import com.dareangel.tmessager.ui.view.bubblechat.OnGestureListenerAdapter

abstract class BubbleChat: OnGestureListenerAdapter() {

    protected abstract val inflater: LayoutInflater
    abstract val rootView: View
    abstract val layoutParams: WindowManager.LayoutParams

    protected abstract fun initialize()

    /**
     * Move without animation
     */
    open fun move(event: MotionEvent, x: Int, y: Int) {}

    /**
     * Move with animation
     */
    open fun move(x: Float, y: Float = -1f, _interpolator: Interpolator, _duration: Long = 1000,
                  onMovingDone: () -> Unit = {}) {}
    open fun updatePosition() {}
    open fun update() {}
    open fun hide() {}
    open fun show() {}
}