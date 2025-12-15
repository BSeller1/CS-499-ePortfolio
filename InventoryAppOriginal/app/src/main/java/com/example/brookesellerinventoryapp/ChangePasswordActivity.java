package com.example.brookesellerinventoryapp;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;


// Change the current user's password.
public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etCurrent, etNew, etConfirm;
    private Button btnSave;

    private LoginDatabase loginDb;
    private String username; // username for password that will be changed

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Back arrow
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        // Bind views
        etCurrent = findViewById(R.id.etCurrent);
        etNew     = findViewById(R.id.etNew);
        etConfirm = findViewById(R.id.etConfirm);
        btnSave   = findViewById(R.id.btnSave);

        loginDb = new LoginDatabase(this);

        // Resolve username to operate on
        username = getIntent().getStringExtra("EXTRA_USERNAME");
        if (TextUtils.isEmpty(username)) {
            SharedPreferences session = getSharedPreferences("auth_session", MODE_PRIVATE);
            username = session.getString("username", null);
        }
        if (TextUtils.isEmpty(username)) {
            username = getFirstUsernameFromDb();
        }

        // If no user can be resolved, disable action
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "No logged-in user found", Toast.LENGTH_LONG).show();
            btnSave.setEnabled(false);
            return;
        }

        btnSave.setOnClickListener(v -> onSave());
    }

    private void onSave() {
        clearErrors();

        String curr = getText(etCurrent);
        String nw   = getText(etNew);
        String conf = getText(etConfirm);

        // password validation
        boolean ok = true;
        if (TextUtils.isEmpty(nw))            { etNew.setError("Required"); ok = false; }
        else if (nw.length() < 8)             { etNew.setError("Must be at least 8 characters"); ok = false; }
        else if (!nw.matches(".*\\d.*"))      { etNew.setError("Add at least one number"); ok = false; }

        if (!nw.equals(conf))                 { etConfirm.setError("Passwords do not match"); ok = false; }
        if (!ok) return;

        // Verify current password against database for this username
        boolean valid = loginDb.validateLogin(username, curr);
        if (!valid) {
            etCurrent.setError("Current password is incorrect");
            etCurrent.requestFocus();
            return;
        }

        // updates database w/ new password
        int rows = loginDb.changePassword(username, nw);
        if (rows <= 0) {
            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    // pull the first username in the table
    private String getFirstUsernameFromDb() {
        Cursor c = null;
        try {
            c = loginDb.getReadableDatabase().query(
                    "users",
                    new String[]{"username"},
                    null, null, null, null,
                    "_id ASC",
                    "1"
            );
            if (c.moveToFirst()) return c.getString(0);
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    private void clearErrors() {
        etCurrent.setError(null);
        etNew.setError(null);
        etConfirm.setError(null);
    }

    private static String getText(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
