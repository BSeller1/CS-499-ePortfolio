package com.example.brookesellerinventoryapp

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.example.brookesellerinventoryapp.InventoryCardAdapter.OnItemAction
import com.google.android.material.appbar.MaterialToolbar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Shows only items with quantity == 0.
 * Reuses InventoryCardAdapter and ItemProductActivity.
 */
class ZeroStockActivity : AppCompatActivity() {
    // grid of product cards
    private lateinit var productGrid: RecyclerView

    // adapter that binds Item objects to cards
    private lateinit var adapter: InventoryCardAdapter

    // database helper
    private lateinit var db: InventoryDatabase

    // single background thread for DB work
    private val io: ExecutorService = Executors.newSingleThreadExecutor()

    // handler to post results back to UI thread
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // layout with AppBar + RecyclerView
        setContentView(R.layout.activity_zero_stock)

        // back arrow closes this screen
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener { finish() }

        // set up DB
        db = InventoryDatabase(this)

        // set up RecyclerView as a 2-column grid
        productGrid = findViewById(R.id.productGrid)
        productGrid.layoutManager = GridLayoutManager(this, 2)
        productGrid.setHasFixedSize(true)

        // add spacing around each card
        val space = (12 * resources.displayMetrics.density).toInt()
        productGrid.addItemDecoration(object : ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect, view: View,
                parent: RecyclerView, state: RecyclerView.State
            ) {
                outRect.set(space, space, space, space)
            }
        })

        // create adapter with actions for click and long press
        adapter = InventoryCardAdapter(object : OnItemAction {
            override fun onClick(item: Item?) {
                // open details screen and pass item data
                val intent = Intent(this@ZeroStockActivity, ItemProductActivity::class.java).apply {
                    putExtra("EXTRA_ID", item?.id ?: -1)
                    putExtra("EXTRA_NAME", item?.name)
                    putExtra("EXTRA_SKU", item?.sku)
                    putExtra("EXTRA_QTY", item?.quantity ?: -1)
                    putExtra("EXTRA_IMAGE", item?.imageUrlOrPath)
                    putExtra("EXTRA_UPC", item?.upc)
                    putExtra("EXTRA_DESCRIPTION", item?.description)
                }
                startActivity(intent)
            }

            override fun onDecrease(item: Item?) {
                // quick −1 on long press, then reload list
                io.execute {
                    db.adjustQuantityBySku(item?.sku, -1)
                    loadZeroStock()
                }
            }
        })
        productGrid.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadZeroStock()
    }

    override fun onDestroy() {
        super.onDestroy()
        io.shutdown()
    }

    // load items with qty == 0 on background thread
    private fun loadZeroStock() {
        io.execute {
            val items = queryZeroItems()
            // push results to adapter on UI thread
            main.post { adapter.submitList(items) }
        }
    }

    // read rows where quantity == 0 and build Item objects
    private fun queryZeroItems(): List<Item> {
        val list = mutableListOf<Item>()
        db.listItemsWithZeroQty().use { c ->
            val iId = c.getColumnIndexOrThrow("_id")
            val iName = c.getColumnIndexOrThrow("name")
            val iUpc = c.getColumnIndexOrThrow("upc")
            val iSku = c.getColumnIndexOrThrow("sku")
            val iDesc = c.getColumnIndexOrThrow("short_description")
            val iQty = c.getColumnIndexOrThrow("quantity")
            while (c.moveToNext()) {
                list.add(
                    Item(
                        c.getLong(iId),  // id
                        c.getString(iName),  // name
                        /* imageUrlOrPath */
                        null,  // no image stored in database
                        c.getString(iSku),  // sku
                        c.getInt(iQty),  // quantity
                        c.getString(iUpc),  // upc
                        c.getString(iDesc) // description
                    )
                )
            }
        }
        return list
    }
}
