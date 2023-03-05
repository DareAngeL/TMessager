package com.dareangel.tmessager.ui.view

import android.content.Intent
import android.os.*
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.dareangel.tmessager.R
import com.dareangel.tmessager.databinding.ActivityMainBinding
import com.dareangel.tmessager.manager.DataManager
import com.dareangel.tmessager.ui.view.fragments.ChatRoomFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@Suppress("FunctionName")
class MainActivity : AppCompatActivity() {

    private lateinit var bindView : ActivityMainBinding
    private lateinit var mDataManager : DataManager

    private var hasSavedInstance = false
    private var hasInitializedAlready = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindView = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindView.root)

        hasSavedInstance = savedInstanceState != null
        _checkPermissions()
    }

    /**
     * Called when user closes the running services.
     * We will also close the application
     */
    private fun forceCloseActivityCallback(): () -> Unit = {
        mDataManager.socket?.unBind()
        mDataManager.msgsDBTable.close(true)
        finishAffinity()
    }

    private fun _init() {
        if (hasSavedInstance || hasInitializedAlready) {
            return
        }

        mDataManager = DataManager(this.javaClass.name)
        _initFragments()
        hasInitializedAlready = true
    }

    private fun _initFragments() {
        // open the messages table
        mDataManager.msgsDBTable.open(this)

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentsRoot,
                ChatRoomFragment(
                    this@MainActivity,
                    mDataManager
                ).also {
                    it.forceCloseActivityCallback = forceCloseActivityCallback()
                })
            setReorderingAllowed(true)
        }.commit()
    }

    private var isAlertDlgShowing = false
    @RequiresApi(Build.VERSION_CODES.M)
    private fun _checkPermissions() {
        //#region: overlay permission
        when(Settings.canDrawOverlays(this)) {
            true -> {
                _init()
            }
            else -> {
                if (!isAlertDlgShowing) {
                    MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                        .setTitle("Permission Request")
                        .setMessage("We need you to grant us the permission to overlay over other app for this app to work")
                        .setCancelable(false)
                        .setPositiveButton("Okay") { _, _ ->
                            isAlertDlgShowing = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                // send user to the device settings
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                startActivity(intent)
                            }
                        }
                        .setNegativeButton("Exit") { _, _ ->
                            finishAffinity()
                        }
                        .show()

                    isAlertDlgShowing = true
                }
            }
        }
        //#region end
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        _checkPermissions()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPause() {
        if (mDataManager.socket?.connected!!)
            mDataManager.socket?.openBubbleChat()

        super.onPause()
    }

    override fun onStop() {
        mDataManager.socket?.unBind()
        mDataManager.msgsDBTable.close(true)
        finishAffinity()
        super.onStop()
    }
}