package com.example.brookesellerinventoryapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity() {

    // Input boxes and buttons on the login screen
    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonCreateLogin: Button
    private lateinit var lengthErrorMessage: TextView

    // Database for users
    private lateinit var db: LoginDatabase

    // Background thread for DB work
    private lateinit var io: ExecutorService
    private lateinit var main: Handler

    // Launchers to ask for permissions at runtime
    private lateinit var notifPermLauncher: ActivityResultLauncher<String>
    private lateinit var smsPermLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Prepare permission request launchers
        notifPermLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // handle notification permission result if needed
        }

        smsPermLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // handle SMS permission result if needed
        }

        // Create database helper and thread handlers
        db = LoginDatabase(this)
        io = Executors.newSingleThreadExecutor()
        main = Handler(Looper.getMainLooper())

        // Ask for needed permissions one time at app start
        requestStartupPermissionsIfNeeded()

        // views from layout
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonCreateLogin = findViewById(R.id.buttonCreateLogin)
        lengthErrorMessage = findViewById(R.id.length_error_message)

        // Start with buttons disabled and hide the length warning
        buttonLogin.isEnabled = false
        buttonCreateLogin.isEnabled = false
        lengthErrorMessage.visibility = View.GONE

        // Watch username/password fields and update UI as the user types
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePasswordLengthUi()
                updateButtonsEnabled()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        editTextUsername.addTextChangedListener(textWatcher)
        editTextPassword.addTextChangedListener(textWatcher)

        // Handle Login button press
        buttonLogin.setOnClickListener { _ ->
            val u = safe(editTextUsername)
            val p = safe(editTextPassword)
            if (hasInvalidInputs(u, p)) return@setOnClickListener

            io.execute {
                val ok = db.validateLogin(u, p)
                if (ok) {
                    getSharedPreferences("auth_session", MODE_PRIVATE).edit {
                        putBoolean("logged_in", true)
                        putString("username", u)
                    }
                }
                main.post {
                    if (ok) {
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Handle Create Account button press
        buttonCreateLogin.setOnClickListener { _ ->
            val u = safe(editTextUsername)
            val p = safe(editTextPassword)
            if (hasInvalidInputs(u, p)) return@setOnClickListener

            io.execute {
                if (db.userExists(u)) {
                    main.post {
                        Toast.makeText(this, "Username already exists.", Toast.LENGTH_SHORT).show()
                    }
                    return@execute
                }

                val rowId = db.createUser(u, p, null)
                if (rowId > 0) {
                    getSharedPreferences("auth_session", MODE_PRIVATE).edit {
                        putBoolean("logged_in", true)
                        putString("username", u)
                    }
                }
                main.post {
                    if (rowId > 0) {
                        Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Could not create account.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        io.shutdown()
    }

    // Ask for notification (Android 13+) and SMS (if device supports it) once
    private fun requestStartupPermissionsIfNeeded() {
        io.execute {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            val askedNotif = prefs.getBoolean("asked_notif_perm_once", false)
            val askedSms = prefs.getBoolean("asked_sms_perm_once", false)

            val needsNotifPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                    !askedNotif

            val needsSmsPerm = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED &&
                    !askedSms

            if (needsNotifPerm) {
                prefs.edit { putBoolean("asked_notif_perm_once", true) }
            }
            if (needsSmsPerm) {
                prefs.edit { putBoolean("asked_sms_perm_once", true) }
            }

            main.post {
                if (needsNotifPerm) {
                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (needsSmsPerm) {
                    smsPermLauncher.launch(Manifest.permission.SEND_SMS)
                }
            }
        }
    }

    // Return true if inputs are bad and show messages next to the fields
    private fun hasInvalidInputs(u: String, p: String): Boolean {
        var isInvalid = false
        if (u.isEmpty()) {
            editTextUsername.error = "Enter username"
            isInvalid = true
        }

        if (p.length < 8) {
            editTextPassword.error = "Password must be at least 8 characters"
            lengthErrorMessage.visibility = View.VISIBLE
            isInvalid = true
        } else {
            editTextPassword.error = null
            lengthErrorMessage.visibility = View.GONE
        }
        return isInvalid
    }

    // Show or hide the password length warning under the box
    private fun updatePasswordLengthUi() {
        val len = editTextPassword.text.length
        lengthErrorMessage.visibility = if (len > 0 && len < 8) View.VISIBLE else View.GONE
    }

    // Turn buttons on when input is good
    private fun updateButtonsEnabled() {
        val u = safe(editTextUsername)
        val p = safe(editTextPassword)
        val enable = u.isNotEmpty() && p.length >= 8
        buttonLogin.isEnabled = enable
        buttonCreateLogin.isEnabled = enable
    }

    // Read text from an EditText and trim it
    private fun safe(e: EditText): String {
        return e.text.toString().trim()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
