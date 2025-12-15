package com.example.brookesellerinventoryapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ItemProductActivity extends AppCompatActivity {

    // Views on the screen
    private ImageView imgProduct;
    private TextView tvTitle, tvDescription, tvSku, tvUpc;
    private EditText etQty;
    private Button btnMinus, btnPlus, btnRemove;
    private InventoryDatabase db;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private long itemId = -1;
    private String name, sku, upc, desc, image;
    private int currentQty = 0;
    private boolean suppressQtyWatcher = false;
    private Runnable pendingSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use the item details layout
        setContentView(R.layout.item_product);

        // Set up database and notification channel
        db = new InventoryDatabase(this);
        Notifications.ensureChannel(getApplicationContext());

        // Find views
        imgProduct    = findViewById(R.id.imgProduct);
        tvTitle       = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvSku         = findViewById(R.id.tvSku);
        tvUpc         = findViewById(R.id.tvUpc);
        etQty         = findViewById(R.id.etQty);
        btnMinus      = findViewById(R.id.btnMinus);
        btnPlus       = findViewById(R.id.btnPlus);
        btnRemove     = findViewById(R.id.btnRemove);

        // Back arrow closes this screen
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        // Read values passed in from the previous screen
        itemId     = getIntent().getLongExtra("EXTRA_ID", -1);
        name       = getIntent().getStringExtra("EXTRA_NAME");
        sku        = getIntent().getStringExtra("EXTRA_SKU");
        currentQty = getIntent().getIntExtra("EXTRA_QTY", 0);
        image      = getIntent().getStringExtra("EXTRA_IMAGE");
        upc        = getIntent().getStringExtra("EXTRA_UPC");
        desc       = getIntent().getStringExtra("EXTRA_DESCRIPTION");

        // Fill the text fields
        tvTitle.setText(name != null ? name : "");
        tvSku.setText(sku != null ? sku : "");
        tvUpc.setText(upc != null ? upc : "");
        tvDescription.setText(desc != null ? desc : "");

        // Show the current quantity in the box
        suppressQtyWatcher = true;
        etQty.setText(String.valueOf(currentQty));
        suppressQtyWatcher = false;

        // Show the image if there is one otherwise have a placeholder
        if (image == null || image.isEmpty()) {
            imgProduct.setImageResource(android.R.drawable.ic_menu_report_image);
        } else {
            Glide.with(this)
                    .load(image)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_menu_report_image)
                    .centerCrop()
                    .into(imgProduct);
        }

        // Only allow numbers in the quantity box, limit to 5 digits
        etQty.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        etQty.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(5) });

        // Plus and minus buttons change the quantity
        btnPlus.setOnClickListener(v -> adjustQty(+1));
        btnMinus.setOnClickListener(v -> adjustQty(-1));

        // Remove button asks first, then deletes the item
        btnRemove.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Remove Item")
                .setMessage("Are you sure you want to remove this item?")
                .setPositiveButton("Yes", (d, w) -> io.execute(() -> {
                    int deleted = 0;
                    // Try delete by id first, then by sku
                    if (itemId > 0) {
                        deleted = db.deleteItemById(itemId);
                    } else if (sku != null && !sku.isEmpty()) {
                        deleted = db.deleteBySku(sku);
                    } else {
                        main.post(() ->
                                Toast.makeText(this, "Unable to remove: no ID/SKU", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    final int result = deleted;
                    // Tell the user and close the screen if delete worked
                    main.post(() -> {
                        if (result > 0) {
                            Toast.makeText(this, "Item removed", Toast.LENGTH_SHORT).show();
                            Intent data = new Intent();
                            data.putExtra("DELETED_ID", itemId);
                            data.putExtra("DELETED_SKU", sku);
                            setResult(RESULT_OK, data);
                            finish();
                        } else {
                            Toast.makeText(this, "Remove failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }))
                .setNegativeButton("Cancel", null)
                .show());

        // When the user types in the quantity box, save after a short pause
        etQty.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (suppressQtyWatcher) return;
                int val = parseOrZero(s.toString());
                if (val < 0) val = 0;
                scheduleSetQty(val);
            }
        });

        // save when the quantity box loses focus
        etQty.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                int val = parseOrZero(etQty.getText().toString());
                setQtyImmediate(val);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the background thread
        io.shutdown();
    }

    // Turn a string into a number, or 0 if it is not a number
    private int parseOrZero(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    // Change the quantity by +1 or -1
    private void adjustQty(int delta) {
        if (sku == null || sku.isEmpty()) {
            Toast.makeText(this, "Missing SKU", Toast.LENGTH_SHORT).show();
            return;
        }
        final int prev = currentQty;
        io.execute(() -> {
            // Update database first
            db.adjustQuantityBySku(sku, delta);
            // Update local copy and keep it >= 0
            int newQty = Math.max(0, prev + delta);
            currentQty = newQty;

            //  show a notification
            if (prev > 0 && newQty == 0) {
                Notifications.notifyZeroStock(getApplicationContext(), itemId, sku, name);
            }

            // Update the UI text
            main.post(() -> {
                suppressQtyWatcher = true;
                etQty.setText(String.valueOf(currentQty));
                etQty.setSelection(etQty.getText().length());
                suppressQtyWatcher = false;
            });
        });
    }

    // Save the typed quantity after a small delay
    private void scheduleSetQty(int newQty) {
        if (pendingSave != null) main.removeCallbacks(pendingSave);
        pendingSave = () -> setQtyImmediate(newQty);
        main.postDelayed(pendingSave, 350);
    }

    // Save the typed quantity
    private void setQtyImmediate(int newQty) {
        newQty = Math.max(0, newQty);
        if (sku == null || sku.isEmpty()) return;

        final int prev = currentQty;
        final int saveVal = newQty;
        io.execute(() -> {
            // Write to database and local copy
            db.updateQuantityBySku(sku, saveVal);
            currentQty = saveVal;

            // show a notification
            if (prev > 0 && saveVal == 0) {
                Notifications.notifyZeroStock(getApplicationContext(), itemId, sku, name);
            }

            // Update the UI text
            main.post(() -> {
                suppressQtyWatcher = true;
                etQty.setText(String.valueOf(currentQty));
                etQty.setSelection(etQty.getText().length());
                suppressQtyWatcher = false;
            });
        });
    }
}
