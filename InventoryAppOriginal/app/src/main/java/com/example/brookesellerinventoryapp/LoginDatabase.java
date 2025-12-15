package com.example.brookesellerinventoryapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class LoginDatabase extends SQLiteOpenHelper {
    // Name of the database file and its version
    private static final String DATABASE_NAME = "login.db";
    private static final int VERSION = 2;

    // Build the helper that opens/creates the database
    public LoginDatabase(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    // Table and column names in one place
    private static final class LoginTable {
        private static final String TABLE = "users";          // table name
        private static final String COL_ID = "_id";           // id
        private static final String COL_USERNAME = "username";// username
        private static final String COL_PASSWORD = "password";// password
        private static final String COL_EMPLOYEE = "employee_name"; // name
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Make the users table the first time the app runs
        String sql = "CREATE TABLE " + LoginTable.TABLE + " (" +
                LoginTable.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                LoginTable.COL_USERNAME + " TEXT NOT NULL UNIQUE, " +
                LoginTable.COL_PASSWORD + " TEXT NOT NULL, " +
                LoginTable.COL_EMPLOYEE + " TEXT" +
                ")";
        db.execSQL(sql);

        // Add an index on username so lookups are faster
        db.execSQL("CREATE INDEX idx_users_username ON " + LoginTable.TABLE +
                " (" + LoginTable.COL_USERNAME + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + LoginTable.TABLE);
        onCreate(db);
    }

    // Insert a new user row and return its row id
    public long createUser(String username, String password, @Nullable String employeeName) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(LoginTable.COL_USERNAME, safe(username));     // store username
        cv.put(LoginTable.COL_PASSWORD, safe(password));     // plaintext for demo only
        if (employeeName != null) cv.put(LoginTable.COL_EMPLOYEE, employeeName);
        return db.insert(LoginTable.TABLE, null, cv);        // returns -1 on failure
    }

    // Check if a username is already in the table
    public boolean userExists(String username) {
        SQLiteDatabase db = getReadableDatabase();
        String[] cols = { LoginTable.COL_ID };
        String sel = LoginTable.COL_USERNAME + " = ?";
        String[] args = { safe(username) };
        try (Cursor c = db.query(LoginTable.TABLE, cols, sel, args, null, null, null)) {
            return c.moveToFirst(); // true if we found a row
        }
    }

    // Check if username and password match a row
    public boolean validateLogin(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        String[] cols = { LoginTable.COL_ID };
        String sel = LoginTable.COL_USERNAME + " = ? AND " + LoginTable.COL_PASSWORD + " = ?";
        String[] args = { safe(username), safe(password) };
        try (Cursor c = db.query(LoginTable.TABLE, cols, sel, args, null, null, null)) {
            return c.moveToFirst(); // true if a match exists
        }
    }

    // Change the password for a username and return
    public int changePassword(String username, String newPassword) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(LoginTable.COL_PASSWORD, safe(newPassword));
        String where = LoginTable.COL_USERNAME + " = ?";
        String[] args = { safe(username) };
        return db.update(LoginTable.TABLE, cv, where, args); // 1 on success, 0 if not found
    }

    // Trim strings and turn null into empty to avoid crashes
    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
