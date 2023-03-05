package com.dareangel.tmessager.ui.view.bubblechat

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.view.View
import android.view.WindowManager
import android.view.animation.Interpolator
import kotlin.math.pow
import kotlin.math.sin

class BubbleChatAnimation(
    private val windowManager: WindowManager,
) {

    private var _x : ValueAnimator? = ValueAnimator.ofInt(0, 1)
    private var y : ValueAnimator? = ValueAnimator.ofInt(0, 1)

    private var mView: View? = null
    var view : View?
        get() = mView
        set(value) { mView = value }

    private var mBubbleChatWinParam: WindowManager.LayoutParams? = null
    var winParam: WindowManager.LayoutParams?
        get() = mBubbleChatWinParam
        set(value) { mBubbleChatWinParam = value }

    private var mDuration: Long = 1000
    var duration: Long
        get() = mDuration
        set(value) { mDuration = value }

    private var mToX: Int = -1
    var toX: Int
        get() = mToX
        set(value) { mToX = value }

    private var mToY: Int = -1
    var toY: Int
        get() = mToY
        set(value) { mToY = value }

    private var mInterpolator: Interpolator = SpringInterpolator()
    var interpolator: Interpolator
        get() = mInterpolator
        set(value) { mInterpolator = value }

    fun isRunning() : Boolean {
        if (_x == null && y == null)
            return false

        return _x!!.isRunning || y!!.isRunning
    }

    fun cancel() {
        _x?.cancel()
        y?.cancel()
    }

    /**
     * Starts the animation
     */
    fun start(onAnimationEnd: () -> Unit = {}) {
        if (isRunning())
            cancel()

        val param = winParam!!
        _x = ValueAnimator.ofInt(param.x, toX).apply {
            duration = mDuration
            interpolator = mInterpolator
            addUpdateListener {
                param.x = it.animatedValue as Int
                windowManager.updateViewLayout(view, param)
            }
        }

        val _toY: Int = if (toY == -1) param.y else toY
        y = ValueAnimator.ofInt(param.y, _toY).apply {
            duration = mDuration
            interpolator = mInterpolator
            addUpdateListener {
                param.y = it.animatedValue as Int
                windowManager.updateViewLayout(view, param)
            }
        }

        AnimatorSet().apply {
            play(_x).with(y)
            addListener(object: Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    onAnimationEnd.invoke()
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }.start()
    }

    class SpringInterpolator(private val factor: Float = 0.3f) : Interpolator {
        override fun getInterpolation(input: Float): Float {
            return (2.0.pow((-10 * input).toDouble()) * sin(2 * Math.PI * (input - factor / 4) / factor) + 1).toFloat()
        }
    }
}