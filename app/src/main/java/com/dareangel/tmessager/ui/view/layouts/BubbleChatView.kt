package com.dareangel.tmessager.ui.view.layouts

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.coordinatorlayout.widget.CoordinatorLayout

class BubbleChatView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : CoordinatorLayout(context, attrs) {

    private var mBackPressedListener: () -> Unit = {}
    var setOnBackPressedListener: () -> Unit
        get() = mBackPressedListener
        set(value) {mBackPressedListener=value}

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event != null && event.keyCode == KeyEvent.KEYCODE_BACK) {
            mBackPressedListener.invoke()
            return true;
        }
        return super.dispatchKeyEvent(event)
    }
}