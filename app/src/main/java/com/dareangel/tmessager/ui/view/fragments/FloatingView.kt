package com.dareangel.tmessager.ui.view.fragments

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.*
import carbon.widget.TextView
import com.dareangel.tmessager.R
import com.dareangel.tmessager.ui.animator.Animator
import com.dareangel.tmessager.ui.view.MainActivity
import com.dareangel.tmessager.util.Utility

class FloatingView(private val cn: Context) {
    private val windowManager = cn.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val floatingView: View = LayoutInflater.from(cn).inflate(R.layout.float_view, null)
    private val msgBadge: TextView = floatingView.findViewById(R.id.msg_badge)

    private val maxWidth = 100
    private var badgeNumCount = 0

    private fun initListeners(layoutParams: WindowManager.LayoutParams) {
        // Set up touch listener
        // Define variables to store the initial and final positions of the view
        var startX = 0f
        var startY = 0f
        var endY = 0f

        // #region: listeners
        floatingView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Get the initial position of the view
                    startX = event.rawX
                    startY = event.rawY
                    endY = layoutParams.y.toFloat()

                    floatingView.alpha = 1f
                }
                MotionEvent.ACTION_MOVE -> {
                    // Calculate the difference between the initial and final positions
                    val dx = event.rawX - startX
                    val dy = event.rawY - startY

                    startX = event.rawX

                    // Get the current width of the view
                    val currWidth = view.width

                    // Calculate the new width of the view by adding or subtracting the difference
                    val newWidth = if (dx < 0) {
                        // If the user moves to the left, increase the width
                        currWidth - dx.toInt()
                    } else {
                        // If the user moves to the right, decrease the width
                        currWidth - dx.toInt()
                    }

                    // Update the position of the view
                    layoutParams.width = if (currWidth < maxWidth) {
                        // the width should not be less than the maximum width provided
                        maxWidth
                    } else {
                        newWidth
                    }

                    layoutParams.y = (endY + dy).toInt()
                    windowManager.updateViewLayout(view, layoutParams)
                }
                MotionEvent.ACTION_UP -> {

                    val dx = event.rawX - startX
                    val dy = event.rawY - startY

                    if (dx == 0f && dy == 0f) {
                        // if the user clicks on the view, perform a click
                        view.performClick()

                        return@setOnTouchListener true
                    }

                    Animator.animateFloatingViewWidth(view, maxWidth, windowManager)
                    minAlpha()
                }
            }

            true // Return true to indicate that the event was handled
        }

        floatingView.setOnClickListener {
            // open the app
            val intent = Intent(cn, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            cn.startActivity(intent)
        }
    }

    private fun minAlpha(delay : Long = 2000L) {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            floatingView.alpha = 0.5f
        }, delay)
    }

    fun onNewMessage() {
        ++badgeNumCount

        floatingView.alpha = 1f
        msgBadge.text = badgeNumCount.toString()
        msgBadge.visibility = View.VISIBLE

        minAlpha()
    }

    fun show() {

        if (badgeNumCount > 0) {
            badgeNumCount = 0
            msgBadge.visibility = View.GONE
        }

        if (floatingView.parent != null)
            return

        val layoutParams =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams(
                    maxWidth,
                    100,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT)
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT)
            }

        val device = Utility.deviceScreen(windowManager)
        // Adjust x and y coordinates to set initial position of floating view.
        layoutParams.x = device.width /2
        layoutParams.y = -(device.height /2 - 100)

        initListeners(layoutParams)
        // Add the view to the window manager
        windowManager.addView(floatingView, layoutParams)

        minAlpha()
    }

    fun hide() {
        badgeNumCount = 0
        msgBadge.visibility = View.GONE

        if (floatingView.parent != null)
            windowManager.removeView(floatingView)
    }
}