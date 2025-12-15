package com.example.brookesellerinventoryapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SmsPermissionActivity extends AppCompatActivity {

    // Code we use to identify the SMS permission request
    private static final int SMS_PERMISSION_CODE = 101;

    // Button to enable/send, and text that shows current status
    private Button btnEnableNotifications;
    private TextView tvPermissionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Grab views from the layout
        btnEnableNotifications = findViewById(R.id.btnEnableNotifications);
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus);

        // If the device has no telephony disable SMS
        if (!canSendSms()) {
            tvPermissionStatus.setText("This device cannot send SMS.");
            btnEnableNotifications.setEnabled(false);
            return;
        }

        // Show current permission state and set button text
        updateUiForPermission();

        // Click = either ask for permission or send a test SMS (if already granted)
        btnEnableNotifications.setOnClickListener(v -> handleSmsPermissionOrSend());
    }

    // Checks hardware capability (phone/SMS support)
    private boolean canSendSms() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    // Updates the label and button text based on if SEND_SMS is granted
    private void updateUiForPermission() {
        boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
        tvPermissionStatus.setText("SMS permission status: " + (granted ? "GRANTED" : "NOT GRANTED"));
        btnEnableNotifications.setText(granted ? "Send Test Notification" : "Enable SMS Notifications");
    }

    // If not granted, ask the user; if granted, send a test message
    private void handleSmsPermissionOrSend() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.SEND_SMS },
                    SMS_PERMISSION_CODE
            );
        } else {
            sendTestNotification();
        }
    }

    // Called after the user answers the permission popup
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (granted) {
                // User allowed SMS
                Toast.makeText(this, "SMS permission granted! Notifications enabled.",
                        Toast.LENGTH_SHORT).show();
                updateUiForPermission();
                sendTestNotification();
            } else {
                // User denied SMS
                Toast.makeText(this, "SMS permission denied. App will continue without notifications.",
                        Toast.LENGTH_LONG).show();
                tvPermissionStatus.setText("SMS permission status: DENIED - App continues without notifications");
            }
        }
    }

    // Sends a one-time test SMS to confirm everything works
    private void sendTestNotification() {
        // checks if permission changed
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission not granted.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Replace this with a real number you control
        String phoneNumber = "+13125551234";
        String message = "Test notification: Low inventory alert!";

        // Get the system SMS manager
        SmsManager sms = getSystemService(SmsManager.class);
        if (sms == null) {
            // pick the default SMS subscription
            int subId = SubscriptionManager.getDefaultSmsSubscriptionId();
            sms = SmsManager.getSmsManagerForSubscriptionId(subId);
        }

        try {
            sms.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "Test notification sent!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Catch and show any errors
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
