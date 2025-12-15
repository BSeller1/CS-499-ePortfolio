package com.example.brookesellerinventoryapp;

import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Simple screen that shows all items from the database in a grid.
public class InventoryDatabaseActivity extends AppCompatActivity {

    private RecyclerView productGrid;           // the grid on screen
    private InventoryCardAdapter adapter;       // binds Item data to cards
    private InventoryDatabase db;               // SQLite helper

    private ExecutorService io;                 // background thread for database
    private Handler main;                       // posts results

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inventory_database_activity); // inflate layout

        db = new InventoryDatabase(this);                     // open/create database
        io = Executors.newSingleThreadExecutor();             // one background worker
        main = new Handler(Looper.getMainLooper());           // main-thread handler

        productGrid = findViewById(R.id.productGrid);         // find RecyclerView
        productGrid.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columns
        productGrid.setHasFixedSize(true);

        // Add equal space around each card (12dp)
        final int space = (int) (12 * getResources().getDisplayMetrics().density);
        productGrid.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override public void getItemOffsets(@NonNull Rect outRect, @NonNull View v,
                                                 @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.set(space, space, space, space);
            }
        });

        // Set up adapter actions: +1, -1, and tap
        adapter = new InventoryCardAdapter(new InventoryCardAdapter.OnItemAction() {
            @Override public void onIncrease(Item item) { updateQtyAsync(item.sku, +1); }
            @Override public void onDecrease(Item item) { updateQtyAsync(item.sku, -1); }
            @Override public void onClick(Item item) { /* open details if you have a screen */ }
        });
        productGrid.setAdapter(adapter);                       // attach adapter

        seedIfEmptyThenLoad();                                 // put sample data if empty, then load
    }

    @Override protected void onResume() {
        super.onResume();
        loadItemsAsync();                                      // refresh list when returning
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (io != null) io.shutdown();                         // stop background worker
    }

   // If table is empty, insert two rows, then load items.
    private void seedIfEmptyThenLoad() {
        io.execute(() -> {
            boolean empty;
            try (Cursor c = db.listAllItems()) {
                empty = (c == null || !c.moveToFirst());
            }
            if (empty) {
                db.createItem("Blue Widget",  "012345678905", "BW-100", "Standard blue widget", 25);
                db.createItem("Green Widget", "012345678912", "GW-200", "Green widget deluxe", 10);
            }
            main.post(this::loadItemsAsync);                   // load items
        });
    }

    // Load all items on a background thread and push to the adapter.
    private void loadItemsAsync() {
        io.execute(() -> {
            List<Item> items = queryAllItems();                // read from DB
            main.post(() -> adapter.submitList(items));        // update UI
        });
    }

    //Change quantity by delta for a given sku, then refresh list.
    private void updateQtyAsync(String sku, int delta) {
        io.execute(() -> {
            db.adjustQuantityBySku(sku, delta);                // +1 or -1
            List<Item> items = queryAllItems();
            main.post(() -> adapter.submitList(items));        // show new values
        });
    }

    // Read all rows from database and convert them into Item objects.
    private List<Item> queryAllItems() {
        ArrayList<Item> list = new ArrayList<>();
        try (Cursor c = db.listAllItems()) {
            if (c != null) {
                // Get column positions once
                int iId    = c.getColumnIndexOrThrow("_id");
                int iName  = c.getColumnIndexOrThrow("name");
                int iSku   = c.getColumnIndexOrThrow("sku");
                int iQty   = c.getColumnIndexOrThrow("quantity");
                int iUpc   = c.getColumnIndexOrThrow("upc");
                int iDesc  = c.getColumnIndexOrThrow("short_description");

                // Build Item objects and add to list
                while (c.moveToNext()) {
                    list.add(new Item(
                            c.getLong(iId),                // id
                            c.getString(iName),            // name
                            null,                          // imageUrlOrPath
                            c.getString(iSku),             // sku
                            c.getInt(iQty),                // quantity
                            c.getString(iUpc),             // upc
                            c.getString(iDesc)             // description
                    ));
                }
            }
        }
        return list;                                           // return
    }
}
