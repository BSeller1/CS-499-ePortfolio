package com.example.brookesellerinventoryapp

import android.content.DialogInterface
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.brookesellerinventoryapp.PasswordListAdapter.OnDeleteClick

class PasswordsFragment : Fragment() {
    private var dbHelper: LoginDatabase? = null
    private var adapter: PasswordListAdapter? = null
    private var cursor: Cursor? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_passwords, container, false)
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)
        val list = v.findViewById<ListView>(R.id.list)
        val empty = v.findViewById<View?>(R.id.empty)
        list.setEmptyView(empty) // show this when list has no rows

        // Set up the database helper
        dbHelper = LoginDatabase(requireContext())

        // Get all rows and plug them into the adapter
        cursor = queryAll()
        adapter = PasswordListAdapter(
            requireContext(),
            cursor,
            { rowId: Long -> this.confirmDelete(rowId) })
        list.setAdapter(adapter)
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (adapter != null) adapter!!.changeCursor(null)
        if (cursor != null && !cursor!!.isClosed) cursor!!.close()
        if (dbHelper != null) dbHelper!!.close()
    }

    // order by username
    private fun queryAll(): Cursor {
        val db = dbHelper!!.readableDatabase
        val cols = arrayOf<String?>("_id", "username", "password")
        return db.query("users", cols, null, null, null, null, "username ASC")
    }

    // Replace the adapter’s data with a fresh cursor
    private fun reload() {
        val newC = queryAll()
        val old = adapter!!.swapCursor(newC)
        if (old != null && !old.isClosed) old.close()
        cursor = newC
    }

    // Ask the user before deleting a row
    private fun confirmDelete(rowId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete user")
            .setMessage("Are you sure you want to delete this entry?")
            .setPositiveButton(
                "Delete",
                DialogInterface.OnClickListener { d: DialogInterface?, w: Int ->
                    deleteById(rowId) // remove from database
                    reload()
                })
            .setNegativeButton("Cancel", null)
            .show()
    }

    // delete user by id
    private fun deleteById(rowId: Long) {
        val db = dbHelper!!.writableDatabase
        db.delete("users", "_id=?", arrayOf<String>(rowId.toString()))
    }
}
