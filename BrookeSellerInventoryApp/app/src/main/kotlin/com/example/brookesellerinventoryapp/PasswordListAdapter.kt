package com.example.brookesellerinventoryapp

import android.R
import android.content.Context
import android.database.Cursor
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

// Shows a list of usernames + passwords from a Cursor.
// Each row also has a trash button you can tap to delete that row.
class PasswordListAdapter // delete listener and pass the Cursor to CursorAdapter
    (context: Context?, c: Cursor?, private val deleteListener: OnDeleteClick?) :
    CursorAdapter(context, c, 0) {
    fun interface OnDeleteClick {
        fun onDelete(rowId: Long)
    }

    private fun dp(ctx: Context, dps: Int): Int {
        val den = ctx.resources.displayMetrics.density
        return (dps * den).roundToInt()
    }

    override fun newView(context: Context, cursor: Cursor?, parent: ViewGroup?): View {
        val root = LinearLayout(context)
        root.orientation = LinearLayout.HORIZONTAL
        root.gravity = Gravity.CENTER_VERTICAL
        root.setPadding(dp(context, 16), dp(context, 8), dp(context, 8), dp(context, 8))
        root.setLayoutParams(
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // Column that holds the two text lines (username + password)
        val texts = LinearLayout(context)
        texts.orientation = LinearLayout.VERTICAL
        val textsLp =
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        root.addView(texts, textsLp)

        // First line: username
        val tvUsername = TextView(context)
        tvUsername.textSize = 16f
        tvUsername.setSingleLine(true)
        tvUsername.ellipsize = TextUtils.TruncateAt.END
        texts.addView(tvUsername)

        // Second line: password
        val tvPassword = TextView(context)
        tvPassword.textSize = 14f
        tvPassword.setSingleLine(true)
        tvPassword.ellipsize = TextUtils.TruncateAt.END
        texts.addView(tvPassword)

        // Trash button on the right
        val btnDelete = ImageButton(
            context, null,
            R.attr.borderlessButtonStyle
        )

        // delete icon
        btnDelete.setImageResource(R.drawable.ic_menu_delete)
        btnDelete.setBackgroundResource(R.drawable.list_selector_background)
        val size = dp(context, 40)
        val btnLp = LinearLayout.LayoutParams(size, size)
        btnDelete.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8))

        // Add the button into the row
        root.addView(btnDelete, btnLp)

        // Store child views in a tag for quick reuse
        val h = ViewHolder()
        h.tvUsername = tvUsername
        h.tvPassword = tvPassword
        h.btnDelete = btnDelete
        root.setTag(h)

        return root
    }

    override fun bindView(view: View, context: Context?, cursor: Cursor) {
        // Get views for this row
        val h = view.getTag() as ViewHolder

        // Read data from the cursor
        val id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
        val username = cursor.getString(cursor.getColumnIndexOrThrow("username"))
        val password = cursor.getString(cursor.getColumnIndexOrThrow("password"))

        // Put text into the row
        h.tvUsername!!.text = username
        h.tvPassword!!.text = password

        // Wire up the trash button
        h.btnDelete!!.setOnClickListener(View.OnClickListener { v: View? ->
            deleteListener?.onDelete(id)
        })
    }

    // holder for fast access to row views
    internal class ViewHolder {
        var tvUsername: TextView? = null
        var tvPassword: TextView? = null
        var btnDelete: ImageButton? = null
    }
}
