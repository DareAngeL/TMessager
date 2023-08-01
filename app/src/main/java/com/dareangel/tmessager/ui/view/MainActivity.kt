package com.dareangel.tmessager.ui.view

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dareangel.tmessager.R
import com.dareangel.tmessager.databinding.ActivityMainBinding
import com.dareangel.tmessager.`object`.ForegroundNotification.CHANNEL_ID
import com.dareangel.tmessager.service.MessagingService
import com.dareangel.tmessager.ui.view.fragments.ChatRoomFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@Suppress("FunctionName")
class MainActivity : AppCompatActivity() {

    private lateinit var bindView : ActivityMainBinding

    private var hasSavedInstance = false
    private var hasInitializedAlready = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindView = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindView.root)

        hasSavedInstance = savedInstanceState != null
        checkPermissions()
    }

    private fun _init() {
        if (hasSavedInstance || hasInitializedAlready) {
            return
        }

        // create the notification channel
        createNotificationChannel()
        // init the fragments
        initFragments()
        hasInitializedAlready = true
    }

    private fun initFragments() {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentsRoot,
                ChatRoomFragment(
                    this@MainActivity
                ))
            setReorderingAllowed(true)
        }.commit()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private var isAlertDlgShowing = false
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermissions() {
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
        checkPermissions()
    }
}