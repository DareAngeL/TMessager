package com.dareangel.tmessager.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.view.WindowInsets
import android.view.WindowManager

object Utility {

    enum class CONN_TYPE {
        OFFLINE, ONLINE
    }

    fun pxToDp(context: Context, px: Int): Int {
        return (px / context.resources.displayMetrics.density).toInt()
    }

    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    fun deviceScreen(winManager: WindowManager) : Size {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val winMetrics = winManager.currentWindowMetrics
            val insets  = winMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            return Size(
                winMetrics.bounds.width() - insets.left - insets.right,
                winMetrics.bounds.height() - insets.top - insets.bottom
            )
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            winManager.defaultDisplay.getMetrics(displayMetrics)
            return Size(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels
            )
        }
    }

    fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    fun getInvokerClassName() : String? {
        val stElements = Thread.currentThread().stackTrace
        var callerClassName: String? = null
        for (i in 1 until stElements.size) {
            val ste = stElements[i]
            if (ste.className != Utility::class.java.name && ste.className.indexOf("java.lang.Thread") != 0) {
                if (callerClassName == null) {
                    callerClassName = ste.className
                } else if (callerClassName != ste.className) {
                    return ste.className
                }
            }
        }
        return null
    }
}