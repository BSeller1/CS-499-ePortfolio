package com.example.brookesellerinventoryapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class Settings extends AppCompatActivity {

    private ActivityResultLauncher<String> notifPermLauncher;

    // views from XML
    private TextView tvPermissionStatus;
    private Button btnEnableNotifications;
    private CircularProgressIndicator smsChecking;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Back arrow
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Find views
        LinearLayout rowManage   = findViewById(R.id.rowManageNotifications);
        LinearLayout rowChange   = findViewById(R.id.rowChangePassword);
        LinearLayout rowFeedback = findViewById(R.id.rowSendFeedback);

        tvPermissionStatus       = findViewById(R.id.tvPermissionStatus);
        btnEnableNotifications   = findViewById(R.id.btnEnableNotifications);
        smsChecking              = findViewById(R.id.smsChecking);

        // Runtime permission launcher
        notifPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    updateNotifPermissionUi();
                    if (granted) {
                        Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
                        ensureNotificationChannel(); // <-- this method is defined below
                    } else {
                        Toast.makeText(this, "Notifications permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

        // Enable SMS Notifications button
        btnEnableNotifications.setOnClickListener(v -> {
            if (needsPostNotificationsPermission() && !hasPostNotificationsPermission()) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Toast.makeText(this, "Notifications already enabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Open system app notification settings
        rowManage.setOnClickListener(v -> {
            Intent i = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(i);
        });

        // Change password and pass current username
        rowChange.setOnClickListener(v -> {
            String username = getSharedPreferences("auth_session", MODE_PRIVATE)
                    .getString("username", null);
            Intent i = new Intent(Settings.this, ChangePasswordActivity.class);
            i.putExtra("EXTRA_USERNAME", username);
            startActivity(i);
        });

        // Send feedback (email)
        rowFeedback.setOnClickListener(v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
            email.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@example.com"});
            email.putExtra(Intent.EXTRA_SUBJECT, "Brooke Seller Inventory - Feedback");
            startActivity(Intent.createChooser(email, "Send feedback"));
        });

        updateNotifPermissionUi();
    }

    /* ----------------- helpers ----------------- */

    private boolean needsPostNotificationsPermission() {
        return true;
    }

    private boolean hasPostNotificationsPermission() {
        if (!needsPostNotificationsPermission()) return true;
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void updateNotifPermissionUi() {
        smsChecking.setVisibility(View.VISIBLE);

        boolean granted = hasPostNotificationsPermission();

        tvPermissionStatus.setText(
                granted ? "SMS/Notification permission: Allowed"
                        : "SMS/Notification permission: Not granted");

        btnEnableNotifications.setEnabled(!granted);
        btnEnableNotifications.setAlpha(granted ? 0.5f : 1f);
        btnEnableNotifications.setText(granted ? "Notifications enabled" : "Enable SMS Notifications");

        smsChecking.setVisibility(View.GONE);
    }

    // Creates a default notification channel.
    private void ensureNotificationChannel() {
        android.app.NotificationManager nm =
                (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = "default";
        if (nm.getNotificationChannel(channelId) == null) {
            android.app.NotificationChannel ch = new android.app.NotificationChannel(
                    channelId,
                    "General",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
            );
            ch.setDescription("General notifications");
            nm.createNotificationChannel(ch);
        }
    }
}
