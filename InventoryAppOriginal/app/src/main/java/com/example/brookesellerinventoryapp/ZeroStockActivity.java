package com.example.brookesellerinventoryapp;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shows only items with quantity == 0.
 * Reuses InventoryCardAdapter and ItemProductActivity.
 */
public class ZeroStockActivity extends AppCompatActivity {

    // grid of product cards
    private RecyclerView productGrid;
    // adapter that binds Item objects to cards
    private InventoryCardAdapter adapter;
    // database helper
    private InventoryDatabase db;
    // single background thread for DB work
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    // handler to post results back to UI thread
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // layout with AppBar + RecyclerView
        setContentView(R.layout.activity_zero_stock);

        // back arrow closes this screen
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        // set up DB
        db = new InventoryDatabase(this);

        // set up RecyclerView as a 2-column grid
        productGrid = findViewById(R.id.productGrid);
        productGrid.setLayoutManager(new GridLayoutManager(this, 2));
        productGrid.setHasFixedSize(true);

        // add spacing around each card
        final int space = (int) (12 * getResources().getDisplayMetrics().density);
        productGrid.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull android.view.View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.set(space, space, space, space);
            }
        });

        // create adapter with actions for click and long press
        adapter = new InventoryCardAdapter(new InventoryCardAdapter.OnItemAction() {
            @Override public void onClick(Item item) {
                // open details screen and pass item data
                Intent i = new Intent(ZeroStockActivity.this, ItemProductActivity.class);
                i.putExtra("EXTRA_ID", item.id);
                i.putExtra("EXTRA_NAME", item.name);
                i.putExtra("EXTRA_SKU", item.sku);
                i.putExtra("EXTRA_QTY", item.quantity);
                i.putExtra("EXTRA_IMAGE", item.imageUrlOrPath);
                i.putExtra("EXTRA_UPC", item.upc);
                i.putExtra("EXTRA_DESCRIPTION", item.description);
                startActivity(i);
            }
            @Override public void onDecrease(Item item) {
                // quick −1 on long press, then reload list
                io.execute(() -> {
                    db.adjustQuantityBySku(item.sku, -1);
                    loadZeroStock();
                });
            }
        });
        productGrid.setAdapter(adapter);
    }

    @Override protected void onResume() {
        super.onResume();
        loadZeroStock();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }

    // load items with qty == 0 on background thread
    private void loadZeroStock() {
        io.execute(() -> {
            List<Item> items = queryZeroItems();
            // push results to adapter on UI thread
            main.post(() -> adapter.submitList(items));
        });
    }

    // read rows where quantity == 0 and build Item objects
    private List<Item> queryZeroItems() {
        ArrayList<Item> list = new ArrayList<>();
        try (Cursor c = db.listItemsWithZeroQty()) {
            if (c != null) {
                int iId   = c.getColumnIndexOrThrow("_id");
                int iName = c.getColumnIndexOrThrow("name");
                int iUpc  = c.getColumnIndexOrThrow("upc");
                int iSku  = c.getColumnIndexOrThrow("sku");
                int iDesc = c.getColumnIndexOrThrow("short_description");
                int iQty  = c.getColumnIndexOrThrow("quantity");
                while (c.moveToNext()) {
                    list.add(new Item(
                            c.getLong(iId),              // id
                            c.getString(iName),          // name
                            /* imageUrlOrPath */ null,   // no image stored in database
                            c.getString(iSku),           // sku
                            c.getInt(iQty),              // quantity
                            c.getString(iUpc),           // upc
                            c.getString(iDesc)           // description
                    ));
                }
            }
        }
        return list;
    }
}
