package com.dareangel.tmessager.ui.animator

import android.animation.ObjectAnimator
import android.view.View

object Animator {

    fun animate(target: View, property: String, value: Float, duration: Long) : ObjectAnimator {
        val animator = ObjectAnimator.ofFloat(target, property, value)
        animator.duration = duration
        animator.start()

        return animator
    }
}