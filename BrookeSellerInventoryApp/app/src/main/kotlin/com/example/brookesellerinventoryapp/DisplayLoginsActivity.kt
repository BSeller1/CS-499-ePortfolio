package com.example.brookesellerinventoryapp

import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import androidx.appcompat.app.AppCompatActivity

// Shows usernames and passwords from the users table.
class DisplayLoginsActivity : AppCompatActivity() {
    private var dbHelper: LoginDatabase? = null
    private var cursor: Cursor? = null // holds the query result

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_password_database)

        // List view on the screen
        val listView = findViewById<ListView>(R.id.list)
        // Shown when there’s no data
        listView.setEmptyView(findViewById<View?>(R.id.empty))

        // Open the DB for reading
        dbHelper = LoginDatabase(this)
        val db = dbHelper!!.readableDatabase

        // Columns we need (_id is required by the adapter)
        val cols: Array<String> = arrayOf("_id", "username", "password")

        // Query all rows, sort by username
        cursor = db.query(
            "users",
            cols,
            null, null, null, null,
            "username ASC"
        )

        // Map DB columns → built-in two-line row (text1, text2)
        val from: Array<String> = arrayOf("username", "password")
        val to = intArrayOf(android.R.id.text1, android.R.id.text2)

        // Hook data to the list
        val adapter = SimpleCursorAdapter(
            this,
            android.R.layout.simple_list_item_2,
            cursor,
            from,
            to,
            0
        )
        listView.setAdapter(adapter)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Close the cursor
        if (cursor != null && !cursor!!.isClosed) cursor!!.close()
    }
}
