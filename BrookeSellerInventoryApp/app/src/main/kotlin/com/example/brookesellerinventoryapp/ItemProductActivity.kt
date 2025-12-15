package com.example.brookesellerinventoryapp

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

class ItemProductActivity : AppCompatActivity() {
    // Views on the screen
    private var imgProduct: ImageView? = null
    private var tvTitle: TextView? = null
    private var tvDescription: TextView? = null
    private var tvSku: TextView? = null
    private var tvUpc: TextView? = null
    private var etQty: EditText? = null
    private var btnMinus: Button? = null
    private var btnPlus: Button? = null
    private var btnRemove: Button? = null
    private var db: InventoryDatabase? = null
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var itemId: Long = -1
    private var name: String? = null
    private var sku: String? = null
    private var upc: String? = null
    private var desc: String? = null
    private var image: String? = null
    private var currentQty = 0
    private var suppressQtyWatcher = false
    private var pendingSave: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use the item details layout
        setContentView(R.layout.item_product)

        // Set up database and notification channel
        db = InventoryDatabase(this)
        Notifications.ensureChannel(applicationContext)

        // Find views
        imgProduct = findViewById(R.id.imgProduct)
        tvTitle = findViewById(R.id.tvTitle)
        tvDescription = findViewById(R.id.tvDescription)
        tvSku = findViewById(R.id.tvSku)
        tvUpc = findViewById(R.id.tvUpc)
        etQty = findViewById(R.id.etQty)
        btnMinus = findViewById(R.id.btnMinus)
        btnPlus = findViewById(R.id.btnPlus)
        btnRemove = findViewById(R.id.btnRemove)

        // Back arrow closes this screen
        val toolbar = findViewById<MaterialToolbar?>(R.id.topAppBar)
        toolbar?.setNavigationOnClickListener { finish() }

        // Read values passed in from the previous screen
        itemId = intent.getLongExtra("EXTRA_ID", -1)
        name = intent.getStringExtra("EXTRA_NAME")
        sku = intent.getStringExtra("EXTRA_SKU")
        currentQty = intent.getIntExtra("EXTRA_QTY", 0)
        image = intent.getStringExtra("EXTRA_IMAGE")
        upc = intent.getStringExtra("EXTRA_UPC")
        desc = intent.getStringExtra("EXTRA_DESCRIPTION")

        // Fill the text fields
        tvTitle?.text = name ?: ""
        tvSku?.text = sku ?: ""
        tvUpc?.text = upc ?: ""
        tvDescription?.text = desc ?: ""

        // Show the current quantity in the box
        suppressQtyWatcher = true
        etQty?.setText(currentQty.toString())
        suppressQtyWatcher = false

        // Show the image if there is one otherwise have a placeholder
        if (image.isNullOrEmpty()) {
            imgProduct?.setImageResource(android.R.drawable.ic_menu_report_image)
        } else {
            Glide.with(this)
                .load(image)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(imgProduct!!)
        }

        // Only allow numbers in the quantity box, limit to 5 digits
        etQty?.keyListener = DigitsKeyListener.getInstance("0123456789")
        etQty?.filters = arrayOf<InputFilter>(LengthFilter(5))

        // Plus and minus buttons change the quantity
        btnPlus?.setOnClickListener { adjustQty(+1) }
        btnMinus?.setOnClickListener { adjustQty(-1) }

        // Remove button asks first, then deletes the item
        btnRemove?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Remove Item")
                .setMessage("Are you sure you want to remove this item?")
                .setPositiveButton(
                    "Yes"
                ) { _: DialogInterface?, _: Int ->
                    io.execute {
                        var deleted = 0
                        // Try delete by id first, then by sku
                        if (itemId > 0) {
                            deleted = db!!.deleteItemById(itemId)
                        } else if (!sku.isNullOrEmpty()) {
                            deleted = db!!.deleteBySku(sku)
                        } else {
                            main.post {
                                Toast.makeText(
                                    this,
                                    "Unable to remove: no ID/SKU",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@execute
                        }
                        val result = deleted
                        // Tell the user and close the screen if delete worked
                        main.post {
                            if (result > 0) {
                                Toast.makeText(this, "Item removed", Toast.LENGTH_SHORT)
                                    .show()
                                val data = Intent().apply {
                                    putExtra("DELETED_ID", itemId)
                                    putExtra("DELETED_SKU", sku)
                                }
                                setResult(RESULT_OK, data)
                                finish()
                            } else {
                                Toast.makeText(this, "Remove failed", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // When the user types in the quantity box, save after a short pause
        etQty?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (suppressQtyWatcher) return
                var value = parseOrZero(s.toString())
                if (value < 0) value = 0
                scheduleSetQty(value)
            }
        })

        // save when the quantity box loses focus
        etQty?.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = parseOrZero(etQty?.text?.toString() ?: "")
                setQtyImmediate(value)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the background thread
        io.shutdown()
    }

    // Turn a string into a number, or 0 if it is not a number
    private fun parseOrZero(s: String): Int {
        return try {
            s.trim().toInt()
        } catch (e: Exception) {
            0
        }
    }

    // Change the quantity by +1 or -1
    private fun adjustQty(delta: Int) {
        if (sku.isNullOrEmpty()) {
            Toast.makeText(this, "Missing SKU", Toast.LENGTH_SHORT).show()
            return
        }
        val prev = currentQty
        io.execute {
            // Update database first
            db!!.adjustQuantityBySku(sku, delta)
            // Update local copy and keep it >= 0
            val newQty = max(0, prev + delta)
            currentQty = newQty

            // show a notification
            if (prev > 0 && newQty == 0) {
                Notifications.notifyZeroStock(applicationContext, itemId, sku, name)
            }

            // Update the UI text
            main.post {
                suppressQtyWatcher = true
                etQty?.setText(currentQty.toString())
                etQty?.setSelection(etQty?.text?.length ?: 0)
                suppressQtyWatcher = false
            }
        }
    }

    // Save the typed quantity after a small delay
    private fun scheduleSetQty(newQty: Int) {
        if (pendingSave != null) main.removeCallbacks(pendingSave!!)
        pendingSave = Runnable { setQtyImmediate(newQty) }
        main.postDelayed(pendingSave!!, 350)
    }

    // Save the typed quantity
    private fun setQtyImmediate(newQty: Int) {
        val clampedQty = max(0, newQty)
        if (sku.isNullOrEmpty()) return

        val prev = currentQty
        val saveVal = clampedQty
        io.execute {
            // Write to database and local copy
            db!!.updateQuantityBySku(sku, saveVal)
            currentQty = saveVal

            // show a notification
            if (prev > 0 && saveVal == 0) {
                Notifications.notifyZeroStock(applicationContext, itemId, sku, name)
            }

            // Update the UI text
            main.post {
                suppressQtyWatcher = true
                etQty?.setText(currentQty.toString())
                etQty?.setSelection(etQty?.text?.length ?: 0)
                suppressQtyWatcher = false
            }
        }
    }
}
