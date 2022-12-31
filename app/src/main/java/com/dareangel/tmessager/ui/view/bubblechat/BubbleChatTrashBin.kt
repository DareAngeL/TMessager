package com.dareangel.tmessager.ui.view.bubblechat

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.Size
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.dareangel.tmessager.R
import com.dareangel.tmessager.data.model.interfaces.BubbleChat

@RequiresApi(Build.VERSION_CODES.O)
class BubbleChatTrashBin(
    private val mContext: Context,
    private val mDeviceScreen: Size
) : BubbleChat() {

    private var mPoint: Point? = null
    private var mArea: Rect? = null

    private var isShowingTrashBin: Boolean = false

    private var mTrashView: View
    private var mParentView: View

    private val HIDING_POSITION = 400f

    private val mInflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    override val inflater: LayoutInflater
        get() = mInflater

    @SuppressLint("InflateParams")
    private val mRootView = mInflater.inflate(R.layout.layout_chat_bubble_bin, null)
    override val rootView: View
        get() = mRootView

    override val layoutParams: WindowManager.LayoutParams
        get() = mParams

    val position: Point?
        get() = mPoint

    val area: Rect?
        get() = mArea

    val hidingPosition: Float
        get() = HIDING_POSITION

    private val mParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )

    init {
        mParentView = mRootView.findViewById(R.id.root)
        mTrashView = mRootView.findViewById(R.id.trash)
        mParams.gravity = Gravity.BOTTOM

        initialize()
        _initListener()
    }

    private fun _initListener() {
        mTrashView.viewTreeObserver.addOnGlobalLayoutListener(
            object: OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mTrashView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    mArea = Rect(
                        mDeviceScreen.width/2 - mTrashView.measuredWidth/2 - 30,
                        mDeviceScreen.height-(mTrashView.measuredHeight*2)-80,
                        mDeviceScreen.width/2 + mTrashView.measuredWidth/2 + 30,
                        mDeviceScreen.height-mTrashView.measuredHeight+80
                    )
                    mPoint = Point(
                        0,
                        (mArea!!.top/2.5 + mTrashView.y-HIDING_POSITION + (mTrashView.measuredHeight/2)).toInt()
                    )
                }
            }
        )
    }

    override fun initialize() {
        mTrashView.translationY = HIDING_POSITION
    }


    override fun update() {
        mRootView.invalidate()
    }

    override fun show() {
        if (isShowingTrashBin)
            return

        isShowingTrashBin = true
        mParentView.alpha = 0f
        mParentView.visibility = View.VISIBLE

        com.dareangel.tmessager.ui.animator.Animator.animate(
            mParentView, "alpha", 1f, 300
        )

        SpringAnimation(mTrashView, DynamicAnimation.TRANSLATION_Y, 0f).apply {
            spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
        }.start()
    }

    override fun hide() {
        isShowingTrashBin = false

        SpringAnimation(mTrashView, DynamicAnimation.TRANSLATION_Y, HIDING_POSITION).apply {
            spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
        }.start()

        com.dareangel.tmessager.ui.animator.Animator.animate(
            mParentView, "alpha", 0f, 300
        ).addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mParentView.visibility = View.GONE
            }
        })
    }
}