package com.dareangel.tmessager.data

import android.content.Context

object UnseenMessagesClient {

    /**
     * Saves the unseen messages to the cache
     */
    fun save(context: Context, str: String) {
        val sharedPref = context.getSharedPreferences("unseens", Context.MODE_PRIVATE)
        sharedPref.edit().putString("unseens", str).apply()
    }

    /**
     * Get the unseen messages that was saveed to the cache.
     */
    fun getUnseens(context: Context) : String? {
        val sharedPref = context.getSharedPreferences("unseens", Context.MODE_PRIVATE)
        return sharedPref.getString("unseens", "")
    }
}