package com.example.brookesellerinventoryapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SmsPermissionActivity : AppCompatActivity() {
    // Button to enable/send, and text that shows current status
    private var btnEnableNotifications: Button? = null
    private var tvPermissionStatus: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Grab views from the layout
        btnEnableNotifications = findViewById<Button>(R.id.btnEnableNotifications)
        tvPermissionStatus = findViewById<TextView>(R.id.tvPermissionStatus)

        // If the device has no telephony disable SMS
        if (!canSendSms()) {
            tvPermissionStatus!!.setText("This device cannot send SMS.")
            btnEnableNotifications!!.setEnabled(false)
            return
        }

        // Show current permission state and set button text
        updateUiForPermission()

        // Click = either ask for permission or send a test SMS (if already granted)
        btnEnableNotifications!!.setOnClickListener(View.OnClickListener { v: View? -> handleSmsPermissionOrSend() })
    }

    // Checks hardware capability (phone/SMS support)
    private fun canSendSms(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    // Updates the label and button text based on if SEND_SMS is granted
    private fun updateUiForPermission() {
        val granted = (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED)
        tvPermissionStatus!!.text = "SMS permission status: " + (if (granted) "GRANTED" else "NOT GRANTED")
        btnEnableNotifications!!.text = if (granted) "Send Test Notification" else "Enable SMS Notifications"
    }

    // If not granted, ask the user; if granted, send a test message
    private fun handleSmsPermissionOrSend() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_CODE
            )
        } else {
            sendTestNotification()
        }
    }

    // Called after the user answers the permission popup
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            val granted = grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED

            if (granted) {
                // User allowed SMS
                Toast.makeText(
                    this, "SMS permission granted! Notifications enabled.",
                    Toast.LENGTH_SHORT
                ).show()
                updateUiForPermission()
                sendTestNotification()
            } else {
                // User denied SMS
                Toast.makeText(
                    this, "SMS permission denied. App will continue without notifications.",
                    Toast.LENGTH_LONG
                ).show()
                tvPermissionStatus!!.setText("SMS permission status: DENIED - App continues without notifications")
            }
        }
    }

    // Sends a one-time test SMS to confirm everything works
    private fun sendTestNotification() {
        // checks if permission changed
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "SMS permission not granted.", Toast.LENGTH_SHORT).show()
            return
        }

        // Replace this with a real number you control
        val phoneNumber = "+13125551234"
        val message = "Test notification: Low inventory alert!"

        // Get the system SMS manager
        var sms = getSystemService<SmsManager?>(SmsManager::class.java)
        if (sms == null) {
            // pick the default SMS subscription
            val subId = SubscriptionManager.getDefaultSmsSubscriptionId()
            sms = SmsManager.getSmsManagerForSubscriptionId(subId)
        }

        try {
            sms.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "Test notification sent!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Catch and show any errors
            Toast.makeText(this, "Failed to send SMS: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        // Code we use to identify the SMS permission request
        private const val SMS_PERMISSION_CODE = 101
    }
}
