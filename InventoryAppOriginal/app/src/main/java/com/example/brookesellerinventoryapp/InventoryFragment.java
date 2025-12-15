package com.example.brookesellerinventoryapp;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InventoryFragment extends Fragment {

    private RecyclerView productGrid;
    private InventoryCardAdapter adapter;
    private InventoryDatabase db;
    private ExecutorService io;
    private Handler main;
    private String currentQuery = "";

    // Opens detail screen
    private ActivityResultLauncher<Intent> openDetails;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        openDetails = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        loadItemsAsync();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        db = new InventoryDatabase(requireContext());
        io = Executors.newSingleThreadExecutor();
        main = new Handler(Looper.getMainLooper());

        productGrid = v.findViewById(R.id.productGrid);
        productGrid.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        productGrid.setHasFixedSize(true);

        final int space = (int) (12 * getResources().getDisplayMetrics().density);
        productGrid.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.set(space, space, space, space);
            }
        });

        adapter = new InventoryCardAdapter(new InventoryCardAdapter.OnItemAction() {
            @Override
            public void onClick(Item item) {
                Intent i = new Intent(requireContext(), ItemProductActivity.class);
                i.putExtra("EXTRA_ID", item.id);
                i.putExtra("EXTRA_NAME", item.name);
                i.putExtra("EXTRA_SKU", item.sku);
                i.putExtra("EXTRA_QTY", item.quantity);
                i.putExtra("EXTRA_IMAGE", item.imageUrlOrPath);
                i.putExtra("EXTRA_UPC", item.upc);
                i.putExtra("EXTRA_DESCRIPTION", item.description);
                openDetails.launch(i);
            }

            @Override
            public void onDecrease(Item item) {
                updateQtyAsync(item.sku, -1);
            }
        });
        productGrid.setAdapter(adapter);

        // Refresh after "Add Item" in MainActivity
        getParentFragmentManager().setFragmentResultListener(
                "refresh_inventory",
                getViewLifecycleOwner(),
                (reqKey, bundle) -> loadItemsAsync()
        );

        //  Search text from MainActivity’s SearchView
        getParentFragmentManager().setFragmentResultListener(
                "inventory_search",
                getViewLifecycleOwner(),
                (reqKey, bundle) -> {
                    currentQuery = safe(bundle.getString("q"));
                    loadItemsAsync();
                }
        );

        seedIfEmptyThenLoad();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadItemsAsync();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (productGrid != null) productGrid.setAdapter(null);
        if (io != null) io.shutdownNow();
    }

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
            loadItemsAsync();
        });
    }

    // Chooses list-all or name-search based on currentQuery
    private void loadItemsAsync() {
        final String q = currentQuery; // capture for background thread
        io.execute(() -> {
            List<Item> items = (q == null || q.trim().isEmpty())
                    ? queryAllItems()
                    : queryItemsByName(q.trim());
            postSubmit(items);
        });
    }

    private void updateQtyAsync(String sku, int delta) {
        io.execute(() -> {
            db.adjustQuantityBySku(sku, delta);
            List<Item> items = (currentQuery == null || currentQuery.trim().isEmpty())
                    ? queryAllItems()
                    : queryItemsByName(currentQuery.trim());
            postSubmit(items);
        });
    }

    private void postSubmit(List<Item> items) {
        if (!isAdded()) return;
        main.post(() -> {
            if (!isAdded()) return;
            adapter.submitList(items);
        });
    }

    private List<Item> queryAllItems() {
        ArrayList<Item> list = new ArrayList<>();
        try (Cursor c = db.listAllItems()) {
            if (c != null) {
                int iId   = c.getColumnIndexOrThrow("_id");
                int iName = c.getColumnIndexOrThrow("name");
                int iUpc  = c.getColumnIndexOrThrow("upc");
                int iSku  = c.getColumnIndexOrThrow("sku");
                int iDesc = c.getColumnIndexOrThrow("short_description");
                int iQty  = c.getColumnIndexOrThrow("quantity");

                while (c.moveToNext()) {
                    list.add(new Item(
                            c.getLong(iId),
                            c.getString(iName),
                            null,
                            c.getString(iSku),
                            c.getInt(iQty),
                            c.getString(iUpc),
                            c.getString(iDesc)
                    ));
                }
            }
        }
        return list;
    }

    // Builds list from db.listItemsByName(query)
    private List<Item> queryItemsByName(String query) {
        ArrayList<Item> list = new ArrayList<>();
        try (Cursor c = db.listItemsByName(query)) {
            if (c != null) {
                int iId   = c.getColumnIndexOrThrow("_id");
                int iName = c.getColumnIndexOrThrow("name");
                int iUpc  = c.getColumnIndexOrThrow("upc");
                int iSku  = c.getColumnIndexOrThrow("sku");
                int iDesc = c.getColumnIndexOrThrow("short_description");
                int iQty  = c.getColumnIndexOrThrow("quantity");

                while (c.moveToNext()) {
                    list.add(new Item(
                            c.getLong(iId),
                            c.getString(iName),
                            null,
                            c.getString(iSku),
                            c.getInt(iQty),
                            c.getString(iUpc),
                            c.getString(iDesc)
                    ));
                }
            }
        }
        return list;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
