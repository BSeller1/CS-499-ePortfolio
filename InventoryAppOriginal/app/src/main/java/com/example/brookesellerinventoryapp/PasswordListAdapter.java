package com.example.brookesellerinventoryapp;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

// Shows a list of usernames + passwords from a Cursor.
// Each row also has a trash button you can tap to delete that row.
public class PasswordListAdapter extends CursorAdapter {

    public interface OnDeleteClick { void onDelete(long rowId); }
    @Nullable private final OnDeleteClick deleteListener;

    // delete listener and pass the Cursor to CursorAdapter
    public PasswordListAdapter(Context context, Cursor c, @Nullable OnDeleteClick onDelete) {
        super(context, c, 0);
        this.deleteListener = onDelete;
    }
    private int dp(Context ctx, int dps) {
        float den = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dps * den);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(dp(context,16), dp(context,8), dp(context,8), dp(context,8));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Column that holds the two text lines (username + password)
        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsLp =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        root.addView(texts, textsLp);

        // First line: username
        TextView tvUsername = new TextView(context);
        tvUsername.setTextSize(16);
        tvUsername.setSingleLine(true);
        tvUsername.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(tvUsername);

        // Second line: password
        TextView tvPassword = new TextView(context);
        tvPassword.setTextSize(14);
        tvPassword.setSingleLine(true);
        tvPassword.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(tvPassword);

        // Trash button on the right
        ImageButton btnDelete = new ImageButton(context, null,
                android.R.attr.borderlessButtonStyle);

        // delete icon
        btnDelete.setImageResource(android.R.drawable.ic_menu_delete);
        btnDelete.setBackgroundResource(android.R.drawable.list_selector_background);
        int size = dp(context, 40);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(size, size);
        btnDelete.setPadding(dp(context,8), dp(context,8), dp(context,8), dp(context,8));

        // Add the button into the row
        root.addView(btnDelete, btnLp);

        // Store child views in a tag for quick reuse
        ViewHolder h = new ViewHolder();
        h.tvUsername = tvUsername;
        h.tvPassword = tvPassword;
        h.btnDelete  = btnDelete;
        root.setTag(h);

        return root;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Get views for this row
        ViewHolder h = (ViewHolder) view.getTag();

        // Read data from the cursor
        long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
        String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
        String password = cursor.getString(cursor.getColumnIndexOrThrow("password"));

        // Put text into the row
        h.tvUsername.setText(username);
        h.tvPassword.setText(password);

        // Wire up the trash button
        h.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(id);
        });
    }

    // holder for fast access to row views
    static class ViewHolder {
        TextView tvUsername, tvPassword;
        ImageButton btnDelete;
    }
}
