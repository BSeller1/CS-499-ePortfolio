package com.example.brookesellerinventoryapp

import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.brookesellerinventoryapp.InventoryCardAdapter.OnItemAction
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class InventoryDatabaseActivity : AppCompatActivity() {
    private lateinit var productGrid: RecyclerView
    private lateinit var adapter: InventoryCardAdapter
    private lateinit var db: InventoryDatabase

    private lateinit var io: ExecutorService
    private lateinit var main: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.inventory_database_activity)

        db = InventoryDatabase(this)
        io = Executors.newSingleThreadExecutor()
        main = Handler(Looper.getMainLooper())

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        setupRecyclerView()
        setupAdapter()

        val fab: FloatingActionButton = findViewById(R.id.fab_add_item)
        fab.setOnClickListener {
            showAddItemDialog()
        }

        seedIfEmptyThenLoad()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_inventory, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // TODO: Implement search functionality
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // TODO: Implement settings screen navigation
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        productGrid = findViewById(R.id.productGrid)
        productGrid.layoutManager = GridLayoutManager(this, 2)
        productGrid.setHasFixedSize(true)

        val space = (12 * resources.displayMetrics.density).toInt()
        productGrid.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect, v: View,
                parent: RecyclerView, state: RecyclerView.State
            ) {
                outRect.set(space, space, space, space)
            }
        })
    }

    private fun setupAdapter() {
        adapter = InventoryCardAdapter(object : OnItemAction {
            override fun onIncrease(item: Item?) {
                updateQtyAsync(item?.sku, +1)
            }

            override fun onDecrease(item: Item?) {
                updateQtyAsync(item?.sku, -1)
            }

            override fun onClick(item: Item?) { /* open details if you have a screen */ }
        })
        productGrid.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // No longer needed here, moved to seedIfEmptyThenLoad
        // loadItemsAsync()
    }

    override fun onDestroy() {
        super.onDestroy()
        io.shutdown()
    }

    private fun showAddItemDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.add_item, null)
        val inputName: TextInputEditText = dialogView.findViewById(R.id.inputName)
        val inputSku: TextInputEditText = dialogView.findViewById(R.id.inputSku)
        val inputUpc: TextInputEditText = dialogView.findViewById(R.id.inputUpc)
        val inputDesc: TextInputEditText = dialogView.findViewById(R.id.inputDesc)
        val inputQty: TextInputEditText = dialogView.findViewById(R.id.inputQty)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add New Item")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.setOnShowListener {
            val positiveButton: Button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = inputName.text.toString().trim()
                val sku = inputSku.text.toString().trim()
                val upc = inputUpc.text.toString().trim()
                val desc = inputDesc.text.toString().trim()
                val qtyString = inputQty.text.toString().trim()

                if (name.isEmpty()) {
                    inputName.error = "Product Name is required"
                    return@setOnClickListener
                }
                if (sku.isEmpty()) {
                    inputSku.error = "SKU is required"
                    return@setOnClickListener
                }
                val quantity = qtyString.toIntOrNull()
                if (quantity == null) {
                    inputQty.error = "A valid quantity is required"
                    return@setOnClickListener
                }

                createNewItemAsync(name, upc, sku, desc, quantity)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun createNewItemAsync(name: String, upc: String, sku: String, desc: String, quantity: Int) {
        io.execute {
            val rowId = db.createItem(name, upc, sku, desc, quantity)
            if (rowId > -1) {
                main.post { Toast.makeText(this, "Item added successfully!", Toast.LENGTH_SHORT).show() }
                loadItemsAsync()
            } else {
                main.post { Toast.makeText(this, "Failed to add item. SKU might already exist.", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun seedIfEmptyThenLoad() {
        io.execute {
            val empty = db.listAllItems().use { c -> !c.moveToFirst() }
            if (empty) {
                db.createItem("Blue Widget", "012345678905", "BW-100", "Standard blue widget", 25)
                db.createItem("Green Widget", "012345678912", "GW-200", "Green widget deluxe", 10)
            }
            loadItemsAsync()
        }
    }

    private fun loadItemsAsync() {
        io.execute {
            val items = queryAllItems()
            main.post { adapter.submitList(items) }
        }
    }

    private fun updateQtyAsync(sku: String?, delta: Int) {
        io.execute {
            db.adjustQuantityBySku(sku, delta)
            loadItemsAsync()
        }
    }

    private fun queryAllItems(): List<Item> {
        val list = mutableListOf<Item>()
        db.listAllItems().use { c ->
            val iId = c.getColumnIndexOrThrow("_id")
            val iName = c.getColumnIndexOrThrow("name")
            val iSku = c.getColumnIndexOrThrow("sku")
            val iQty = c.getColumnIndexOrThrow("quantity")
            val iUpc = c.getColumnIndexOrThrow("upc")
            val iDesc = c.getColumnIndexOrThrow("short_description")

            while (c.moveToNext()) {
                list.add(
                    Item(
                        c.getLong(iId),
                        c.getString(iName),
                        null,
                        c.getString(iSku),
                        c.getInt(iQty),
                        c.getString(iUpc),
                        c.getString(iDesc)
                    )
                )
            }
        }
        return list
    }
}
