package com.example.brookesellerinventoryapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.CircularProgressIndicator
import androidx.core.net.toUri

class Settings : AppCompatActivity() {
    private var notifPermLauncher: ActivityResultLauncher<String?>? = null

    // views from XML
    private var tvPermissionStatus: TextView? = null
    private var btnEnableNotifications: Button? = null
    private var smsChecking: CircularProgressIndicator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Back arrow
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener(View.OnClickListener { v: View? -> finish() })

        // Find views
        val rowManage = findViewById<LinearLayout>(R.id.rowManageNotifications)
        val rowChange = findViewById<LinearLayout>(R.id.rowChangePassword)
        val rowFeedback = findViewById<LinearLayout>(R.id.rowSendFeedback)

        tvPermissionStatus = findViewById<TextView>(R.id.tvPermissionStatus)
        btnEnableNotifications = findViewById<Button>(R.id.btnEnableNotifications)
        smsChecking = findViewById<CircularProgressIndicator>(R.id.smsChecking)

        // Runtime permission launcher
        notifPermLauncher = registerForActivityResult<String?, Boolean?>(
            RequestPermission(),
            ActivityResultCallback { granted: Boolean? ->
                updateNotifPermissionUi()
                if (granted == true) {
                    Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
                    ensureNotificationChannel() // <-- this method is defined below
                } else {
                    Toast.makeText(this, "Notifications permission denied", Toast.LENGTH_SHORT)
                        .show()
                }
            })

        // Enable SMS Notifications button
        btnEnableNotifications!!.setOnClickListener(View.OnClickListener { v: View? ->
            if (needsPostNotificationsPermission() && !hasPostNotificationsPermission()) {
                notifPermLauncher!!.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Toast.makeText(this, "Notifications already enabled", Toast.LENGTH_SHORT).show()
            }
        })

        // Open system app notification settings
        rowManage.setOnClickListener(View.OnClickListener { v: View? ->
            val i = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName())
            startActivity(i)
        })

        // Change password and pass current username
        rowChange.setOnClickListener(View.OnClickListener { v: View? ->
            val username = getSharedPreferences("auth_session", MODE_PRIVATE)
                .getString("username", null)
            val i = Intent(this@Settings, ChangePasswordActivity::class.java)
            i.putExtra("EXTRA_USERNAME", username)
            startActivity(i)
        })

        // Send feedback (email)
        rowFeedback.setOnClickListener(View.OnClickListener { v: View? ->
            val email = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
            email.putExtra(Intent.EXTRA_EMAIL, arrayOf<String>("support@example.com"))
            email.putExtra(Intent.EXTRA_SUBJECT, "Brooke Seller Inventory - Feedback")
            startActivity(Intent.createChooser(email, "Send feedback"))
        })

        updateNotifPermissionUi()
    }

    /* ----------------- helpers ----------------- */
    private fun needsPostNotificationsPermission(): Boolean {
        return true
    }

    private fun hasPostNotificationsPermission(): Boolean {
        if (!needsPostNotificationsPermission()) return true
        return (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun updateNotifPermissionUi() {
        smsChecking!!.visibility = View.VISIBLE

        val granted = hasPostNotificationsPermission()

        tvPermissionStatus!!.text = if (granted)
            "SMS/Notification permission: Allowed"
        else
            "SMS/Notification permission: Not granted"

        btnEnableNotifications!!.setEnabled(!granted)
        btnEnableNotifications!!.setAlpha(if (granted) 0.5f else 1f)
        btnEnableNotifications!!.text = if (granted) "Notifications enabled" else "Enable SMS Notifications"

        smsChecking!!.visibility = View.GONE
    }

    // Creates a default notification channel.
    private fun ensureNotificationChannel() {
        val nm =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "default"
        if (nm.getNotificationChannel(channelId) == null) {
            val ch = NotificationChannel(
                channelId,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            ch.description = "General notifications"
            nm.createNotificationChannel(ch)
        }
    }
}
