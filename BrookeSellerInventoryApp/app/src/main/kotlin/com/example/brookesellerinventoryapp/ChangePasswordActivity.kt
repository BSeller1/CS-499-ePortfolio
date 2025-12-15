package com.example.brookesellerinventoryapp

import android.database.Cursor
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

// Change the current user's password.
class ChangePasswordActivity : AppCompatActivity() {
    private var etCurrent: EditText? = null
    private var etNew: EditText? = null
    private var etConfirm: EditText? = null
    private var btnSave: Button? = null

    private var loginDb: LoginDatabase? = null
    private var username: String? = null // username for password that will be changed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        // Back arrow
        val toolbar = findViewById<MaterialToolbar?>(R.id.topAppBar)
        toolbar?.setNavigationOnClickListener(View.OnClickListener { v: View? -> finish() })

        // Bind views
        etCurrent = findViewById<EditText>(R.id.etCurrent)
        etNew = findViewById<EditText>(R.id.etNew)
        etConfirm = findViewById<EditText>(R.id.etConfirm)
        btnSave = findViewById<Button>(R.id.btnSave)

        loginDb = LoginDatabase(this)

        // Resolve username to operate on
        username = intent.getStringExtra("EXTRA_USERNAME")
        if (TextUtils.isEmpty(username)) {
            val session = getSharedPreferences("auth_session", MODE_PRIVATE)
            username = session.getString("username", null)
        }
        if (TextUtils.isEmpty(username)) {
            username = this.firstUsernameFromDb
        }

        // If no user can be resolved, disable action
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "No logged-in user found", Toast.LENGTH_LONG).show()
            btnSave!!.setEnabled(false)
            return
        }

        btnSave!!.setOnClickListener(View.OnClickListener { v: View? -> onSave() })
    }

    private fun onSave() {
        clearErrors()

        val curr: String = Companion.getText(etCurrent!!)
        val nw: String = Companion.getText(etNew!!)
        val conf: String = Companion.getText(etConfirm!!)

        // password validation
        var ok = true
        if (TextUtils.isEmpty(nw)) {
            etNew!!.error = "Required"
            ok = false
        } else if (nw.length < 8) {
            etNew!!.error = "Must be at least 8 characters"
            ok = false
        } else if (!nw.matches(".*\\d.*".toRegex())) {
            etNew!!.error = "Add at least one number"
            ok = false
        }

        if (nw != conf) {
            etConfirm!!.error = "Passwords do not match"
            ok = false
        }
        if (!ok) return

        // Verify current password against database for this username
        val valid = loginDb!!.validateLogin(username, curr)
        if (!valid) {
            etCurrent!!.error = "Current password is incorrect"
            etCurrent!!.requestFocus()
            return
        }

        // updates database w/ new password
        val rows = loginDb!!.changePassword(username, nw)
        if (rows <= 0) {
            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }

    private val firstUsernameFromDb: String?
        // pull the first username in the table
        get() {
            var c: Cursor? = null
            try {
                c = loginDb!!.readableDatabase.query(
                    "users",
                    arrayOf<String>("username"),
                    null, null, null, null,
                    "_id ASC",
                    "1"
                )
                if (c.moveToFirst()) return c.getString(0)
            } finally {
                c?.close()
            }
            return null
        }

    private fun clearErrors() {
        etCurrent!!.error = null
        etNew!!.error = null
        etConfirm!!.error = null
    }

    companion object {
        private fun getText(et: EditText): String {
            return if (et.getText() == null) "" else et.getText().toString().trim { it <= ' ' }
        }
    }
}
