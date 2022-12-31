package com.dareangel.tmessager.ui.view.messagesdisplayer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Canvas
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EdgeEffect
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs

/**
 * EdgeEffectFactory to achieve over scrolling effect
 */
class MessagesRecyclerViewOverScrollEffect : RecyclerView.EdgeEffectFactory() {

    private val TAG = "MessagesRecyclerViewEdge"

    private val mCoroutineScope = CoroutineScope(Job() + Dispatchers.Default)

    private var mLoadMoreOffsetRatio = 1.5f // the default offset ratio
    var loadMoreOffsetRatio: Float
        get() = mLoadMoreOffsetRatio
        set(value) {mLoadMoreOffsetRatio=value}

    companion object {
        const val MAX_PULL_VALUE = 150
    }

    override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
        return object : EdgeEffect(view.context) {

            private var recyclerAnim : ObjectAnimator? = null
            private var pullToLoadMoreViewAnim : ObjectAnimator? = null
            private val recycler = view as MessagesRecyclerview2

            private val DEFAULT_STRENGTH = 0.5f
            private var OVERSCROLL_STRENGTH = DEFAULT_STRENGTH

            override fun draw(canvas: Canvas?): Boolean {
                return false
            }

            override fun isFinished(): Boolean {
                return recyclerAnim?.isRunning?.not() ?: true
            }

            /**
             * Called when the user over scrolls the recyclerview
             */
            override fun onPull(deltaDistance: Float) {
                super.onPull(deltaDistance)
                pull(deltaDistance)
            }

            /**
             * Called when the user over scrolls the recyclerview
             */
            override fun onPull(deltaDistance: Float, displacement: Float) {
                super.onPull(deltaDistance, displacement)
                pull(deltaDistance)
            }

            /**
             * Called when the user released from overscrolling
             */
            override fun onRelease() {
                super.onRelease()
                release()
            }

            /**
             * This will be called when the user scrolls
             * too fast then reached at the end while having a velocity
             */
            override fun onAbsorb(velocity: Int) {
                super.onAbsorb(velocity)
                absorb(velocity)
            }

            private fun pull(deltaDistance: Float) {
                val distanceValue = distanceValue(deltaDistance)
                // Translate
                recycler.translationY += distanceValue
                recycler.pullToLoadMoreView!!.translationY += distanceValue

                val pullValue: Float = recycler.translationY / MAX_PULL_VALUE
                recycler.apply {
                    setIsOverScrolling(true)
                    pullToLoadMoreListener?.onPulling(pullValue)
                }
            }

            private fun release() {
                recycler.setIsOverScrolling(false)
                OVERSCROLL_STRENGTH = DEFAULT_STRENGTH
                recycler.messagesLayoutManager?.canScroll(true)
                if (recycler.translationY != 0f) {
                    recycler.pullToLoadMoreListener?.onRelease()
                    // load more data if the translation surpassed the 100 value mark
                    if (recycler.translationY >= 100 &&
                        !recycler.messageAdapter!!.isDataFullyLoaded()
                    ) {
                        animateOnRelease(
                            true,
                            recycler.pullToLoadMoreView!!.measuredHeight.toFloat() / loadMoreOffsetRatio
                        )

                        // animate to 0 translation Y
                        val timer = Timer()
                        val timerTask = object : TimerTask() {
                            override fun run() {
                                animateOnRelease(false, 0f)
                                mCoroutineScope.launch(Dispatchers.Main) {
                                    recycler.loadMoreData()
                                }
                            }
                        }
                        timer.schedule(timerTask, 1500)
                    } else {
                        animateOnRelease(
                            false,
                            0f
                        )
                    }
                }
            }

            private fun animateOnRelease(isOnLoadMore: Boolean, toValue: Float) {
                recyclerAnim = fling(recycler, toValue)
                pullToLoadMoreViewAnim = fling(recycler.pullToLoadMoreView!!, toValue)

                mCoroutineScope.launch(Dispatchers.Main) {
                    if (!isOnLoadMore) {
                        recyclerAnim?.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                recycler.pullToLoadMoreListener?.onClose()
                            }
                        })
                    }

                    AnimatorSet().apply {
                        play(recyclerAnim).with(pullToLoadMoreViewAnim)
                    }.start()
                }
            }

            private fun absorb(velocity: Int) {
                recyclerAnim?.cancel()
                val directionValue = if (direction == DIRECTION_BOTTOM) -1 else 1
                val overscrollAnim = fling(recycler, directionValue * velocity * 0.01f)
                val backscrollAnim = fling(
                    recycler,
                    0f
                )

                AnimatorSet().apply {
                    play(backscrollAnim).after(overscrollAnim)
                }.start()
            }

            private fun distanceValue(deltaDistance: Float) : Float {
                val _direction = directionValue()
                OVERSCROLL_STRENGTH -= if (OVERSCROLL_STRENGTH < 0.2f) -0.01f else 0.02f
                return _direction * recycler.width * deltaDistance * (OVERSCROLL_STRENGTH)
            }

            private fun directionValue() = if (direction == DIRECTION_BOTTOM) -1 else 1

            private fun fling(view: View, value: Float) =
                ObjectAnimator.ofFloat(view, "translationY", value).apply {
                    duration = if (value != 0f) abs(value.toLong()) else 300
                    interpolator = AccelerateDecelerateInterpolator()
                }
        }
    }
}