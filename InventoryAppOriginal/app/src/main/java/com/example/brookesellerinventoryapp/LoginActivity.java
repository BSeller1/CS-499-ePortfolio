package com.example.brookesellerinventoryapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    // Input boxes and buttons on the login screen
    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button buttonLogin;
    private Button buttonCreateLogin;
    private TextView lengthErrorMessage;

    // Database for users
    private LoginDatabase db;
    private static final String TAG = "LoginActivity";

    // Launchers to ask for permissions at runtime
    private ActivityResultLauncher<String> notifPermLauncher;
    private ActivityResultLauncher<String> smsPermLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Show the login layout
        setContentView(R.layout.activity_login);

        // Prepare permission request launchers
        notifPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> { /* optional */ });

        smsPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> { /* optional */ });

        // Ask for needed permissions one time at app start
        requestStartupPermissionsIfNeeded();

        // views from layout
        editTextUsername   = findViewById(R.id.editTextUsername);
        editTextPassword   = findViewById(R.id.editTextPassword);
        buttonLogin        = findViewById(R.id.buttonLogin);
        buttonCreateLogin  = findViewById(R.id.buttonCreateLogin);
        lengthErrorMessage = findViewById(R.id.length_error_message);

        // Create database helper
        db = new LoginDatabase(this);

        // Start with buttons disabled and hide the length warning
        if (buttonLogin != null) buttonLogin.setEnabled(false);
        if (buttonCreateLogin != null) buttonCreateLogin.setEnabled(false);
        if (lengthErrorMessage != null) lengthErrorMessage.setVisibility(View.GONE);

        // Watch username/password fields and update UI as the user types
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordLengthUi();   // show/hide the "too short" message
                updateButtonsEnabled();     // enable/disable buttons
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        if (editTextUsername != null) editTextUsername.addTextChangedListener(watcher);
        if (editTextPassword != null) editTextPassword.addTextChangedListener(watcher);

        // Handle Login button press
        if (buttonLogin != null) {
            buttonLogin.setOnClickListener(v -> {
                Log.d(TAG, "Login button clicked");

                // Read inputs
                String u = safe(editTextUsername);
                String p = safe(editTextPassword);

                // Block bad input
                if (hasInvalidInputs(u, p)) return;

                // Check credentials against the database
                boolean ok = db.validateLogin(u, p);
                Log.d(TAG, "validateLogin(" + u + ", ******) -> " + ok);

                if (ok) {
                    // Save a simple session flag and the username
                    getSharedPreferences("auth_session", MODE_PRIVATE)
                            .edit()
                            .putBoolean("logged_in", true)
                            .putString("username", u)
                            .apply();

                    // Go to the main screen
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    // Wrong username or password
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Handle Create Account button press
        if (buttonCreateLogin != null) {
            buttonCreateLogin.setOnClickListener(v -> {
                Log.d(TAG, "Create Login button clicked");

                // Read inputs safely
                String u = safe(editTextUsername);
                String p = safe(editTextPassword);

                // Block bad input early
                if (hasInvalidInputs(u, p)) return;

                // Stop if username is taken
                if (db.userExists(u)) {
                    Toast.makeText(this, "Username already exists.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create the user in the database
                long rowId = db.createUser(u, p, null);
                boolean okNow = db.validateLogin(u, p);
                Log.d(TAG, "createUser rowId=" + rowId + " | validateNow=" + okNow);

                if (rowId > 0) {
                    // Log the user in right after creating the account
                    getSharedPreferences("auth_session", MODE_PRIVATE)
                            .edit()
                            .putBoolean("logged_in", true)
                            .putString("username", u)
                            .apply();

                    // Go to the main screen
                    Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    // Insert failed
                    Toast.makeText(this, "Could not create account.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Ask for notification (Android 13+) and SMS (if device supports it) once
    private void requestStartupPermissionsIfNeeded() {
        SharedPreferences p = getSharedPreferences("settings", MODE_PRIVATE);
        boolean askedNotif = p.getBoolean("asked_notif_perm_once", false);
        boolean askedSms   = p.getBoolean("asked_sms_perm_once", false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED && !askedNotif) {
                p.edit().putBoolean("asked_notif_perm_once", true).apply();
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Only ask for SEND_SMS if the device can send SMS
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            if (checkSelfPermission(Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED && !askedSms) {
                p.edit().putBoolean("asked_sms_perm_once", true).apply();
                smsPermLauncher.launch(Manifest.permission.SEND_SMS);
            }
        }
    }

    // Return true if inputs are bad and show messages next to the fields
    private boolean hasInvalidInputs(String u, String p) {
        boolean invalid = false;
        if (TextUtils.isEmpty(u)) {
            if (editTextUsername != null) editTextUsername.setError("Enter username");
            invalid = true;
        }
        if (p.length() < 8) {
            if (editTextPassword != null) editTextPassword.setError("Password must be at least 8 characters");
            if (lengthErrorMessage != null) lengthErrorMessage.setVisibility(View.VISIBLE);
            invalid = true;
        } else {
            if (lengthErrorMessage != null) lengthErrorMessage.setVisibility(View.GONE);
        }
        return invalid;
    }

    // Show or hide the password length warning under the box
    private void updatePasswordLengthUi() {
        if (editTextPassword == null) return;
        CharSequence pwd = editTextPassword.getText();
        int len = (pwd == null) ? 0 : pwd.length();
        if (lengthErrorMessage == null) return;

        if (len == 0) {
            lengthErrorMessage.setVisibility(View.GONE);
        } else if (len < 8) {
            lengthErrorMessage.setVisibility(View.VISIBLE);
        } else {
            lengthErrorMessage.setVisibility(View.GONE);
        }
    }

    // Turn buttons on when input is good
    private void updateButtonsEnabled() {
        String u = safe(editTextUsername);
        String p = safe(editTextPassword);
        boolean enable = !u.isEmpty() && p.length() >= 8;
        if (buttonLogin != null) buttonLogin.setEnabled(enable);
        if (buttonCreateLogin != null) buttonCreateLogin.setEnabled(enable);
    }

    // Read text from an EditText and trim it, return empty string if null
    private String safe(EditText e) {
        if (e == null) return "";
        CharSequence cs = e.getText();
        return cs == null ? "" : cs.toString().trim();
    }
}
