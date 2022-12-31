package com.dareangel.tmessager.ui.view.bubblechat

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.GestureDetectorCompat
import com.dareangel.tmessager.R
import com.dareangel.tmessager.data.model.interfaces.BubbleChat
import com.dareangel.tmessager.data.model.interfaces.UnseenMessagesListener
import com.dareangel.tmessager.util.Utility
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.O)
class BubbleChatHead(
    private val mContext: Context,
    private val mWinManager: WindowManager,
    private val mDeviceScreenSpace: Rect,
    private val mTrashBin: BubbleChatTrashBin,
    private val mChatBody: BubbleChatBody
) : BubbleChat(), OnTouchListener, UnseenMessagesListener {

    private val mGestureDetector: GestureDetectorCompat = GestureDetectorCompat(mContext, this)

    private var mMoveDirection : Int = 0
    private val MOVE_UP_DIRECTION = -1
    private val MOVE_DOWN_DIRECTION = 1

    private val SIDE_OVERLAP_OFFSET = 8 // offset of how overlap the chat bubble to the side of the screen

    private var deltaDistanceX: Float = 0f
    private var deltaDistanceY: Float = 0f
    private var isOnTouching = false
    private var isOnClick = false
    private var isOnFling: Boolean = false
    private var isInTrashBin: Boolean = false
    private var isOnHide = false
    private lateinit var mBubbleChatPosition: Point
    private var isTrashAbsorbingBChat: Boolean = false
    private val mParamHolder = WindowManager.LayoutParams()

    private val touchStartPointer = Point(-1, -1)
    private val mBubbleChatSize = Utility.dpToPx(mContext, 30)

    private val mParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT
    )

    private var mInflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    override val inflater: LayoutInflater
        get() = mInflater

    @SuppressLint("InflateParams")
    private var mRootView = mInflater.inflate(R.layout.layout_chatbubble_head, null)
    override val rootView : View
        get() = mRootView

    override val layoutParams: WindowManager.LayoutParams
        get() = mParams

    private val mParentView: View = mRootView.findViewById(R.id.bubbleRoot)
    private val mUnseenIndicator: View = mRootView.findViewById(R.id.unseenIndicator)
    private val mUnseenText: TextView = mRootView.findViewById(R.id.unseenTxt)

    init {
        initialize()
        mRootView.setOnTouchListener(this)
        mChatBody.unseenMessagesListener = this
    }

    /**
     * Initializes the position of this bubble chat head
     */
    override fun initialize() {
        mBubbleChatPosition = Point(
            mDeviceScreenSpace.right-(mBubbleChatSize-SIDE_OVERLAP_OFFSET),
            -200
        )

        mParams.x = this.mBubbleChatPosition.x
        mParams.y = this.mBubbleChatPosition.y
    }

    override fun move(event: MotionEvent, x: Int, y: Int) {
        mParams.x = x
        mParams.y = y
        updatePosition()
    }

    override fun move(x: Float, y: Float, _interpolator: Interpolator, _duration: Long,
                      onMovingDone: () -> Unit)
    {
        BubbleChatAnimation(mWinManager).apply {
            view = mRootView
            winParam = mParams
            duration = _duration
            interpolator = _interpolator
            toX = x
            toY = y
        }.start(onMovingDone)
    }

    override fun onNewUnseenMessage(count: Int) {
        // shows the chat head when
        // there's a new message from the other peer
        // if it's on hide.
        if (isOnHide)
            show()

        if (mUnseenIndicator.visibility == View.GONE) {
            mUnseenIndicator.visibility = View.VISIBLE
            mUnseenText.text = count.toString()
            return
        }

        mUnseenText.text = count.toString()
    }

    override fun updatePosition() {
        mWinManager.updateViewLayout(mRootView, mParams)
    }

    override fun update() {
        mRootView.invalidate()
    }

    override fun show() {
        var toX = -1

        initialize() // reset the position of the chat head
        toX = mParams.x
        mParams.x += mBubbleChatSize
        updatePosition()
        mParentView.alpha = 1f
        mParentView.visibility = View.VISIBLE
        isOnHide = false

        move(toX.toFloat(), -1f, OvershootInterpolator(), 300)
    }

    override fun hide() {
        isInTrashBin = false
        isOnHide = true
        move(
            0f,
            mTrashBin.position!!.y + mTrashBin.hidingPosition,
            AccelerateInterpolator(),
            300
        )
        com.dareangel.tmessager.ui.animator.Animator.animate(
            mParentView, "alpha", 0f, 300
        ).addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mParentView.visibility = View.GONE
            }
        })
    }

    private fun openChat() {
        val _screenHeight = mDeviceScreenSpace.bottom * 2
        val y = _screenHeight - mChatBody.height
        val newX = mDeviceScreenSpace.right-(mBubbleChatSize-SIDE_OVERLAP_OFFSET)
        val newY = -(mDeviceScreenSpace.bottom - y)
        mUnseenIndicator.visibility = View.GONE

        move(newX.toFloat(), newY.toFloat(), OvershootInterpolator())
        mChatBody.show()
    }

    private fun closeChat() {
        mChatBody.hide()
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        isOnClick = true
        if (!mChatBody.isOpen) {
            openChat()
            return super.onSingleTapUp(e)
        }

        closeChat()
        return super.onSingleTapUp(e)
    }

    /**
     * Determine if the move point is in the area of trash bin
     */
    private fun isMovePointInTrashBinArea(event: MotionEvent) : Boolean =
        event.rawY >= mTrashBin.area!!.top &&
        event.rawX >= mTrashBin.area!!.left &&
        event.rawX <= mTrashBin.area!!.right

    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (isInTrashBin)
            return false

        isOnFling = true
        var _toY = if (mMoveDirection == MOVE_UP_DIRECTION)
            mParams.y - abs(velocityY * 0.8f)
        else
            mParams.y + abs(velocityY * 0.8f)

        // adjust the y direction if it'll exceed the screen space.
        if (mMoveDirection == MOVE_UP_DIRECTION &&
            _toY < mDeviceScreenSpace.top
        ) {
            _toY = mDeviceScreenSpace.top.toFloat()
        } else if (mMoveDirection == MOVE_DOWN_DIRECTION &&
            _toY > mDeviceScreenSpace.bottom
        ) {
            _toY = mDeviceScreenSpace.bottom.toFloat()
        }

        when {
            // move the bubble chat to the right
            mParams.x > 0 -> {
                move(
                    mDeviceScreenSpace.right-(mBubbleChatSize-SIDE_OVERLAP_OFFSET).toFloat(),
                    _toY,
                    OvershootInterpolator()
                )
            }
            // move the bubble chat to the left
            mParams.x < 0 -> {
                move(
                    mDeviceScreenSpace.left+(mBubbleChatSize-SIDE_OVERLAP_OFFSET).toFloat(),
                    _toY,
                    OvershootInterpolator()
                )
            }
        }

        return super.onFling(e1, e2, velocityX, velocityY)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        mGestureDetector.onTouchEvent(event!!)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isOnTouching = true

                touchStartPointer.x = event.rawX.toInt()
                touchStartPointer.y = event.rawY.toInt()

                this.mBubbleChatPosition.x = mParams.x
                this.mBubbleChatPosition.y = mParams.y
            }

            MotionEvent.ACTION_MOVE -> {
                mTrashBin.show()

                // calculate the delta x and y distance of the move
                deltaDistanceX = event.rawX - touchStartPointer.x
                deltaDistanceY = event.rawY - touchStartPointer.y
                // know the direction of the move
                mMoveDirection = if (event.rawY > touchStartPointer.y)
                    MOVE_DOWN_DIRECTION
                else
                    MOVE_UP_DIRECTION

                mParamHolder.x = this.mBubbleChatPosition.x + (deltaDistanceX).toInt()
                mParamHolder.y = this.mBubbleChatPosition.y + (deltaDistanceY).toInt()

                val moving = mBubbleChatPosition.x != mParams.x ||
                             mBubbleChatPosition.y != mParams.y

                when {
                    // if it reach the trash view area absorb the bubble chat view to the trash bin
                    isMovePointInTrashBinArea(event) -> {
                        if (!isInTrashBin) {
                            isInTrashBin = true
                            isTrashAbsorbingBChat = true
                            // move to trash bin
                            move(
                                0f,
                                mTrashBin.position!!.y.toFloat(),
                                BubbleChatAnimation.SpringInterpolator()
                            ) {
                                // on moving done
                                isTrashAbsorbingBChat = false
                                if (!isOnTouching) {
                                    hide()
                                    mTrashBin.hide()
                                }
                            }
                        }

                        return true
                    }
                    // if it reach the {right} side of the device screen dont move the bubble chat
                    mParams.x >= mDeviceScreenSpace.right -> {
                        mParams.x = mDeviceScreenSpace.right - mBubbleChatSize
                        return true
                    }
                    // if it reach the {left} side of the device screen dont move the bubble chat
                    mParams.x <= mDeviceScreenSpace.left -> {
                        mParams.x = mDeviceScreenSpace.left + mBubbleChatSize
                        return true
                    }
                    // if it reach the {bottom} side of the device screen dont move the bubble chat
                    mParams.y >= mDeviceScreenSpace.bottom -> {
                        mParams.y = mDeviceScreenSpace.bottom - mBubbleChatSize
                        return true
                    }
                    // if it reach the {top} side of the device screen dont move the bubble chat
                    mParams.y <= mDeviceScreenSpace.top -> {
                        mParams.y = mDeviceScreenSpace.top + mBubbleChatSize
                        return true
                    }
                }

                // #region: animate the bubble chat view out from the trash bin
                if (isInTrashBin) {
                    isInTrashBin = false

                    move(
                        mParamHolder.x.toFloat(),
                        mParamHolder.y.toFloat(),
                        AccelerateDecelerateInterpolator(),
                        300
                    )

                    return true
                }
                // #region end

                if (mChatBody!!.isOpen && moving)
                    closeChat()

                // #region: moves the bubble chat view
                move(
                    event,
                    this.mBubbleChatPosition.x + (event.rawX - touchStartPointer.x).toInt(),
                    this.mBubbleChatPosition.y + (event.rawY - touchStartPointer.y).toInt()
                )
                // #region end
            }

            MotionEvent.ACTION_UP -> {
                isOnTouching = false

                if (!isTrashAbsorbingBChat) {
                    if (isInTrashBin)
                        hide()

                    mTrashBin.hide()
                }

                if (isOnFling) {
                    isOnFling = false
                    return true
                }

                if (isInTrashBin)
                    return true

                if (isOnClick) {
                    isOnClick = false
                    return false
                }

                when {
                    // move the bubble chat to the right
                    mParams.x > 0 -> {
                        move(
                            mDeviceScreenSpace.right-(mBubbleChatSize-SIDE_OVERLAP_OFFSET).toFloat(),
                            -1f,
                            OvershootInterpolator(),
                        )
                    }
                    // move the bubble chat to the left
                    mParams.x < 0 -> {
                        move(
                            mDeviceScreenSpace.left+(mBubbleChatSize-SIDE_OVERLAP_OFFSET).toFloat(),
                            -1f,
                            OvershootInterpolator()
                        )
                    }
                }
            }
        }

        return true
    }
}