package com.dareangel.tmessager.ui.animator

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator

object Animator {

    fun animate(target: View, property: String, value: Float, duration: Long) : ObjectAnimator {
        val animator = ObjectAnimator.ofFloat(target, property, value)
        animator.duration = duration
        animator.start()

        return animator
    }

    fun animateFloatingViewWidth(view: View, targetWidth: Int, winManager: WindowManager) {
        val initialWidth = view.width

        // Create a ValueAnimator
        val animator = ValueAnimator.ofInt(initialWidth, targetWidth)

        // Set up interpolator and duration as desired
        animator.interpolator = OvershootInterpolator()
        animator.duration = 500L

        // Update the layout params during each frame update
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int

            val layoutParams = view.layoutParams.apply {
                width = value
            }

            view.layoutParams = layoutParams
            winManager.updateViewLayout(view, layoutParams)
        }

        // Start the animation
        animator.start()
    }
}