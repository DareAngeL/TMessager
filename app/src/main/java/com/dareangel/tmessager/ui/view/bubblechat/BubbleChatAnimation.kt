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

    private var x : ValueAnimator? = null
    private var y : ValueAnimator? = null

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

    private var mToX: Float = -1f
    var toX: Float
        get() = mToX
        set(value) { mToX = value }

    private var mToY: Float = -1f
    var toY: Float
        get() = mToY
        set(value) { mToY = value }

    private var mInterpolator: Interpolator = SpringInterpolator()
    var interpolator: Interpolator
        get() = mInterpolator
        set(value) { mInterpolator = value }

    fun isRunning() : Boolean {
        if (x == null && y == null)
            return false

        return x!!.isRunning || y!!.isRunning
    }

    fun cancel() {
        x?.cancel()
        y?.cancel()
    }

    fun start(onAnimationEnd: () -> Unit = {}) {
        val param = winParam!!
        x = ValueAnimator.ofFloat(param.x.toFloat(), toX).apply {
            duration = mDuration
            interpolator = mInterpolator
            addUpdateListener {
                param.x = (it.animatedValue as Float).toInt()
                windowManager.updateViewLayout(view, param)
            }
        }

        val _toY: Float = if (toY == -1f) param.y.toFloat() else toY
        y = ValueAnimator.ofFloat(param.y.toFloat(), _toY).apply {
            duration = mDuration
            interpolator = mInterpolator
            addUpdateListener {
                param.y = (it.animatedValue as Float).toInt()
                windowManager.updateViewLayout(view, param)
            }
        }

        AnimatorSet().apply {
            play(x).with(y)
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