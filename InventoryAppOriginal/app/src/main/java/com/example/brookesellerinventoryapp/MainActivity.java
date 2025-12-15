package com.example.brookesellerinventoryapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Main screen with two tabs, a search in the toolbar, and a + button to add items
public class MainActivity extends AppCompatActivity {

    // tabs and pager
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fab;
    private InventoryDatabase db;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Notifications.ensureChannel(this);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set up views
        db        = new InventoryDatabase(this);
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        fab       = findViewById(R.id.fab);

        // two pages: Inventory (0) and Passwords (1)
        viewPager.setAdapter(new TabsAdapter(this));
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(position == 0 ? "Inventory" : "Passwords")
        ).attach();

        // + button opens the add item pop-up
        fab.setOnClickListener(v -> showAddItemDialog());
    }

    // show the add item pop-up and save
    private void showAddItemDialog() {
        // pop-up layout with text fields
        final var view = getLayoutInflater().inflate(R.layout.add_item, null, false);

        // wrappers that show errors
        final TextInputLayout tilName = view.findViewById(R.id.tilName);
        final TextInputLayout tilSku  = view.findViewById(R.id.tilSku);
        final TextInputLayout tilUpc  = view.findViewById(R.id.tilUpc);
        final TextInputLayout tilDesc = view.findViewById(R.id.tilDesc);
        final TextInputLayout tilQty  = view.findViewById(R.id.tilQty);

        // actual inputs
        final TextInputEditText inputName = view.findViewById(R.id.inputName);
        final TextInputEditText inputSku  = view.findViewById(R.id.inputSku);
        final TextInputEditText inputUpc  = view.findViewById(R.id.inputUpc);
        final TextInputEditText inputDesc = view.findViewById(R.id.inputDesc);
        final TextInputEditText inputQty  = view.findViewById(R.id.inputQty);

        // build the dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Item")
                .setView(view)
                .setPositiveButton("Save", null) // we handle click below
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        // handle Save button after dialog shows
        dialog.setOnShowListener(dlg -> {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(btn -> {
                // clear old errors
                tilName.setError(null);
                tilSku.setError(null);
                tilUpc.setError(null);
                tilQty.setError(null);

                // read input
                String name = safe(getText(inputName));
                String sku  = safe(getText(inputSku));
                String upc  = safe(getText(inputUpc));
                String desc = safe(getText(inputDesc));
                String qtyS = safe(getText(inputQty));

                // must fill these
                boolean ok = true;
                if (TextUtils.isEmpty(name)) { tilName.setError("Required"); ok = false; }
                if (TextUtils.isEmpty(sku))  { tilSku.setError("Required");  ok = false; }
                if (TextUtils.isEmpty(upc))  { tilUpc.setError("Required");  ok = false; }

                // make number, not negative
                int qty = 0;
                try {
                    qty = Integer.parseInt(qtyS.isEmpty() ? "0" : qtyS);
                    if (qty < 0) { tilQty.setError("Must be ≥ 0"); ok = false; }
                } catch (NumberFormatException e) {
                    tilQty.setError("Invalid number");
                    ok = false;
                }

                // stop if not ok
                if (!ok) return;

                // do DB work on background thread
                final int finalQty = qty;
                io.execute(() -> {
                    // check if sku/upc already used
                    boolean skuExists = db.itemExistsBySku(sku);
                    boolean upcExists = db.itemExistsByUpc(upc);

                    if (skuExists || upcExists) {
                        // show which one is taken
                        main.post(() -> {
                            if (skuExists) tilSku.setError("SKU already exists");
                            if (upcExists) tilUpc.setError("UPC already exists");
                        });
                        return;
                    }

                    // save item
                    long rowId = db.createItem(name, upc, sku, desc, finalQty);

                    // back to UI
                    main.post(() -> {
                        if (rowId > 0) {
                            Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            // tell the list to refresh
                            notifyInventoryToRefresh();
                        } else {
                            Toast.makeText(this, "Insert failed", Toast.LENGTH_LONG).show();
                        }
                    });
                });
            });
        });

        // show the pop-up
        dialog.show();
    }

    // tell the InventoryFragment to reload
    private void notifyInventoryToRefresh() {
        main.post(() -> getSupportFragmentManager()
                .setFragmentResult("refresh_inventory", new Bundle()));
    }

    // get text or empty
    private static String getText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }

    // trim or empty
    private static String safe(String s) { return s == null ? "" : s.trim(); }

    // make the top-right menu (includes SearchView)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate menu_inventory (it should contain an item with id=action_search using a SearchView)
        getMenuInflater().inflate(R.menu.menu_inventory, menu);

        // wire search
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView sv = (SearchView) searchItem.getActionView();
        sv.setQueryHint("Search by name...");

        // send query text to InventoryFragment as the user types
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { send(q); return true; }
            @Override public boolean onQueryTextChange(String q) { send(q); return true; }
            private void send(String q) {
                Bundle b = new Bundle();
                b.putString("q", q == null ? "" : q);
                getSupportFragmentManager().setFragmentResult("inventory_search", b);
            }
        });

        return true;
    }

    // handle menu taps
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // open settings screen
            startActivity(new Intent(this, Settings.class));
            return true;

        } else if (id == R.id.action_about) {
            // simple toast
            Toast.makeText(this, "About tapped", Toast.LENGTH_SHORT).show();
            return true;

        } else if (id == R.id.action_notifications) {
            // open zero-stock screen
            startActivity(new Intent(this, ZeroStockActivity.class));
            return true;

        } else if (id == R.id.action_logout) {
            // wipe session and go to login
            getSharedPreferences("auth_session", MODE_PRIVATE).edit().clear().apply();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // stop background thread
        io.shutdown();
    }
}
