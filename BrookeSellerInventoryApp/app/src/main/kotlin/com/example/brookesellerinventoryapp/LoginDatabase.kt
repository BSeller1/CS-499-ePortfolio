package com.example.brookesellerinventoryapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LoginDatabase  // Build the helper that opens/creates the database
    (context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {
    // Table and column names in one place
    private object LoginTable {
        const val TABLE = "users" // table name
        const val COL_ID = "_id" // id
        const val COL_USERNAME = "username" // username
        const val COL_PASSWORD = "password" // password
        const val COL_EMPLOYEE = "employee_name" // name
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Make the users table the first time the app runs
        val sql = "CREATE TABLE " + LoginTable.TABLE + " (" +
                LoginTable.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                LoginTable.COL_USERNAME + " TEXT NOT NULL UNIQUE, " +
                LoginTable.COL_PASSWORD + " TEXT NOT NULL, " +
                LoginTable.COL_EMPLOYEE + " TEXT" +
                ")"
        db.execSQL(sql)

        // Add an index on username so lookups are faster
        db.execSQL(
            "CREATE INDEX idx_users_username ON " + LoginTable.TABLE +
                    " (" + LoginTable.COL_USERNAME + ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + LoginTable.TABLE)
        onCreate(db)
    }

    // Insert a new user row and return its row id
    fun createUser(username: String?, password: String?, employeeName: String?): Long {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put(LoginTable.COL_USERNAME, safe(username)) // store username
        cv.put(LoginTable.COL_PASSWORD, safe(password)) // plaintext for demo only
        if (employeeName != null) cv.put(LoginTable.COL_EMPLOYEE, employeeName)
        return db.insert(LoginTable.TABLE, null, cv) // returns -1 on failure
    }

    // Check if a username is already in the table
    fun userExists(username: String?): Boolean {
        val db = readableDatabase
        val cols = arrayOf<String?>(LoginTable.COL_ID)
        val sel: String = LoginTable.COL_USERNAME + " = ?"
        val args = arrayOf<String?>(safe(username))
        db.query(LoginTable.TABLE, cols, sel, args, null, null, null).use { c ->
            return c.moveToFirst() // true if we found a row
        }
    }

    // Check if username and password match a row
    fun validateLogin(username: String?, password: String?): Boolean {
        val db = readableDatabase
        val cols = arrayOf<String?>(LoginTable.COL_ID)
        val sel: String = LoginTable.COL_USERNAME + " = ? AND " + LoginTable.COL_PASSWORD + " = ?"
        val args = arrayOf<String?>(safe(username), safe(password))
        db.query(LoginTable.TABLE, cols, sel, args, null, null, null).use { c ->
            return c.moveToFirst() // true if a match exists
        }
    }

    // Change the password for a username and return
    fun changePassword(username: String?, newPassword: String?): Int {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put(LoginTable.COL_PASSWORD, safe(newPassword))
        val where: String = LoginTable.COL_USERNAME + " = ?"
        val args = arrayOf<String?>(safe(username))
        return db.update(LoginTable.TABLE, cv, where, args) // 1 on success, 0 if not found
    }

    // Trim strings and turn null into empty to avoid crashes
    private fun safe(s: String?): String {
        return s?.trim { it <= ' ' } ?: ""
    }

    companion object {
        // Name of the database file and its version
        private const val DATABASE_NAME = "login.db"
        private const val VERSION = 2
    }
}
