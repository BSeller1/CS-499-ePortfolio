package com.example.brookesellerinventoryapp;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.*;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class PasswordsFragment extends Fragment {
    private LoginDatabase dbHelper;
    private PasswordListAdapter adapter;
    private Cursor cursor;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_passwords, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        ListView list = v.findViewById(R.id.list);
        View empty = v.findViewById(R.id.empty);
        list.setEmptyView(empty); // show this when list has no rows

        // Set up the database helper
        dbHelper = new LoginDatabase(requireContext());

        // Get all rows and plug them into the adapter
        cursor = queryAll();
        adapter = new PasswordListAdapter(requireContext(), cursor, this::confirmDelete);
        list.setAdapter(adapter);
    }

    @Override public void onResume() {
        super.onResume();
        reload();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) adapter.changeCursor(null);
        if (cursor != null && !cursor.isClosed()) cursor.close();
        if (dbHelper != null) dbHelper.close();
    }

    // order by username
    private Cursor queryAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] cols = {"_id","username","password"};
        return db.query("users", cols, null, null, null, null, "username ASC");
    }

    // Replace the adapter’s data with a fresh cursor
    private void reload() {
        Cursor newC = queryAll();
        Cursor old = adapter.swapCursor(newC);
        if (old != null && !old.isClosed()) old.close();
        cursor = newC;
    }

    // Ask the user before deleting a row
    private void confirmDelete(long rowId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete user")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton("Delete", (d, w) -> {
                    deleteById(rowId); // remove from database
                    reload();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // delete user by id
    private void deleteById(long rowId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("users", "_id=?", new String[]{ String.valueOf(rowId) });
    }
}
