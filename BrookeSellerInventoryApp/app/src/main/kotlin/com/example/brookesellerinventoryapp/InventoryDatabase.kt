package com.example.brookesellerinventoryapp

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlin.math.max

// Simple SQLite helper for the inventory.
class InventoryDatabase(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {
    // Table + column names
    private object ItemsTable {
        const val TABLE = "items"
        const val COL_ID = "_id"
        const val COL_NAME = "name"
        const val COL_UPC = "upc"
        const val COL_SKU = "sku"
        const val COL_SHORT_DESC = "short_description"
        const val COL_QTY = "quantity"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Make the items table
        val sql = "CREATE TABLE " + ItemsTable.TABLE + " (" +
                ItemsTable.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +  // auto id
                ItemsTable.COL_NAME + " TEXT NOT NULL, " +  // product name
                ItemsTable.COL_UPC + " TEXT NOT NULL UNIQUE, " +  // unique upc
                ItemsTable.COL_SKU + " TEXT NOT NULL UNIQUE, " +  // unique sku
                ItemsTable.COL_SHORT_DESC + " TEXT, " +  // short description
                ItemsTable.COL_QTY + " INTEGER NOT NULL DEFAULT 0 CHECK(" +  // >= 0 only
                ItemsTable.COL_QTY + " >= 0)" +
                ")"
        db.execSQL(sql)

        // Speed up name searches
        db.execSQL(
            "CREATE INDEX idx_items_name ON " + ItemsTable.TABLE +
                    "(" + ItemsTable.COL_NAME + ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop and recreate table if the version changes
        db.execSQL("DROP TABLE IF EXISTS " + ItemsTable.TABLE)
        onCreate(db)
    }

    // ---------------- CRUD ----------------
    /**
     * Add one item.
     * @return row id (>0) if it worked, or -1 if it failed (like duplicate upc/sku)
     */
    fun createItem(
        name: String?, upc: String?, sku: String?,
        shortDescription: String?, quantity: Int
    ): Long {
        val db = getWritableDatabase()
        val cv = ContentValues()
        cv.put(ItemsTable.COL_NAME, safe(name))
        cv.put(ItemsTable.COL_UPC, safe(upc))
        cv.put(ItemsTable.COL_SKU, safe(sku))
        if (shortDescription != null) cv.put(
            ItemsTable.COL_SHORT_DESC,
            shortDescription.trim { it <= ' ' })
        cv.put(ItemsTable.COL_QTY, max(0, quantity)) // never below 0
        return db.insert(ItemsTable.TABLE, null, cv)
    }

    // Check if a sku already exists.
    fun itemExistsBySku(sku: String?): Boolean {
        val db = readableDatabase
        val cols = arrayOf<String?>(ItemsTable.COL_ID)
        val sel: String = ItemsTable.COL_SKU + " = ?"
        val args = arrayOf(safe(sku))
        db.query(ItemsTable.TABLE, cols, sel, args, null, null, null).use { c ->
            return c.moveToFirst()
        }
    }

    // Check if a upc already exists
    fun itemExistsByUpc(upc: String?): Boolean {
        val db = readableDatabase
        val cols = arrayOf(ItemsTable.COL_ID)
        val sel: String = ItemsTable.COL_UPC + " = ?"
        val args = arrayOf(safe(upc))
        db.query(ItemsTable.TABLE, cols, sel, args, null, null, null).use { c ->
            return c.moveToFirst()
        }
    }

    // Get one item by sku
    fun getItemBySku(sku: String?): Cursor {
        val db = readableDatabase
        val cols = arrayOf<String?>(
            ItemsTable.COL_ID, ItemsTable.COL_NAME, ItemsTable.COL_UPC,
            ItemsTable.COL_SKU, ItemsTable.COL_SHORT_DESC, ItemsTable.COL_QTY
        )
        val sel: String = ItemsTable.COL_SKU + " = ?"
        val args = arrayOf<String?>(safe(sku))
        return db.query(ItemsTable.TABLE, cols, sel, args, null, null, null)
    }


    // Get all items, sorted by name
    fun listAllItems(): Cursor {
        val db = readableDatabase
        val cols = arrayOf<String?>(
            ItemsTable.COL_ID, ItemsTable.COL_NAME, ItemsTable.COL_UPC,
            ItemsTable.COL_SKU, ItemsTable.COL_SHORT_DESC, ItemsTable.COL_QTY
        )
        return db.query(
            ItemsTable.TABLE, cols, null, null, null, null,
            ItemsTable.COL_NAME + " ASC"
        )
    }

    // Find items by name, case-insensitive
    fun listItemsByName(query: String?): Cursor {
        val db = readableDatabase
        val q = "%" + escapeLike(query) + "%"
        return db.query(
            ItemsTable.TABLE,
            arrayOf<String>(
                ItemsTable.COL_ID, ItemsTable.COL_NAME, ItemsTable.COL_UPC,
                ItemsTable.COL_SKU, ItemsTable.COL_SHORT_DESC, ItemsTable.COL_QTY
            ),
            ItemsTable.COL_NAME + " LIKE ? ESCAPE '\\'",
            arrayOf<String>(q),
            null, null,
            ItemsTable.COL_NAME + " COLLATE NOCASE ASC"
        )
    }

    // ---------------- Quantity & updates ----------------
    // Set quantity by sku
    fun updateQuantityBySku(sku: String?, newQuantity: Int): Int {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put(ItemsTable.COL_QTY, max(0, newQuantity))
        val where: String = ItemsTable.COL_SKU + " = ?"
        val args = arrayOf<String?>(safe(sku))
        return db.update(ItemsTable.TABLE, cv, where, args)
    }

    // Add or subtract from quantity by sku
    fun adjustQuantityBySku(sku: String?, delta: Int): Int {
        var updated = 0
        getItemBySku(sku).use { c ->
            if (c.moveToFirst()) {
                val current = c.getInt(c.getColumnIndexOrThrow(ItemsTable.COL_QTY))
                updated = updateQuantityBySku(sku, max(0, current + delta))
            }
        }
        return updated
    }

    // ---------------- Deletes ----------------
    // Delete one row by id
    fun deleteItemById(id: Long): Int {
        val db = writableDatabase
        val where: String = ItemsTable.COL_ID + " = ?"
        val args = arrayOf<String?>(id.toString())
        return db.delete(ItemsTable.TABLE, where, args)
    }

    // Delete one row by sku
    fun deleteBySku(sku: String?): Int {
        val db = writableDatabase
        val where: String = ItemsTable.COL_SKU + " = ?"
        val args = arrayOf<String?>(safe(sku))
        return db.delete(ItemsTable.TABLE, where, args)
    }

    // Get only items with qty == 0
    fun listItemsWithZeroQty(): Cursor {
        val db = readableDatabase
        val cols = arrayOf<String?>(
            "_id", "name", "upc", "sku", "short_description", "quantity"
        )
        val sel = "quantity = 0"
        return db.query("items", cols, sel, null, null, null, "name COLLATE NOCASE ASC")
    }

    // ---------------- Helpers ----------------
    //Trim strings and avoid nulls.
    private fun safe(s: String?): String {
        return s?.trim { it <= ' ' } ?: ""
    }

    companion object {
        // Name of the .db file and its version
        private const val DATABASE_NAME = "inventory.db"
        private const val VERSION = 1 // change this if you change the table

        // ---------------- Search ----------------
        // Escape %, _ and \ so LIKE searches don’t break.
        private fun escapeLike(s: String?): String {
            if (s == null) return ""
            return s.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
        }
    }
}
