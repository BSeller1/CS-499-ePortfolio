package com.example.brookesellerinventoryapp;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import androidx.appcompat.app.AppCompatActivity;

// Shows usernames and passwords from the users table.
public class DisplayLoginsActivity extends AppCompatActivity {

    private LoginDatabase dbHelper;
    private Cursor cursor; // holds the query result

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_password_database);

        // List view on the screen
        ListView listView = findViewById(R.id.list);
        // Shown when there’s no data
        listView.setEmptyView(findViewById(R.id.empty));

        // Open the DB for reading
        dbHelper = new LoginDatabase(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Columns we need (_id is required by the adapter)
        String[] cols = new String[] { "_id", "username", "password" };

        // Query all rows, sort by username
        cursor = db.query(
                "users",
                cols,
                null, null, null, null,
                "username ASC"
        );

        // Map DB columns → built-in two-line row (text1, text2)
        String[] from = new String[] { "username", "password" };
        int[] to = new int[] { android.R.id.text1, android.R.id.text2 };

        // Hook data to the list
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_2,
                cursor,
                from,
                to,
                0
        );
        listView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close the cursor
        if (cursor != null && !cursor.isClosed()) cursor.close();
    }
}
