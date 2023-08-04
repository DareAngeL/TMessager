package com.dareangel.tmessager.util

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.content.ContextCompat.getSystemService
import com.google.gson.Gson


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

    fun pingInternet(onPingResult: (Boolean) -> Unit) {
        val runtime = Runtime.getRuntime()
        try {
            val process = runtime.exec("/system/bin/ping -c 1 8.8.8.8")
            val exitValue = process.waitFor()
            if (exitValue == 0) {
                // ping was successful
                onPingResult(true)
            } else {
                // ping failed
                onPingResult(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun getFromCache(cn: Context, prefName: String, key: String, cl: Class<*>) : Any? {
        val sharedPref: SharedPreferences =
            cn.getSharedPreferences(prefName, Context.MODE_PRIVATE)

        val objJSON = sharedPref.getString(key, null)
        return if (objJSON != null) {
            Gson().fromJson(objJSON, cl)
        } else {
            null
        }
    }

    fun saveToCache(cn: Context, prefName: String, key: String, any: Any) {
        val sharedPref: SharedPreferences =
            cn.getSharedPreferences(prefName, Context.MODE_PRIVATE)

        val objJSON = Gson().toJson(any)

        sharedPref.edit().putString(key, objJSON).apply()
    }
}