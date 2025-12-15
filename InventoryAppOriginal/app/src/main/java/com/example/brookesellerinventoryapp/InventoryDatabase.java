package com.example.brookesellerinventoryapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

// Simple SQLite helper for the inventory.
public class InventoryDatabase extends SQLiteOpenHelper {
    // Name of the .db file and its version
    private static final String DATABASE_NAME = "inventory.db";
    private static final int VERSION = 1; // change this if you change the table

    public InventoryDatabase(Context context) {
        // Builds/opens the database file
        super(context, DATABASE_NAME, null, VERSION);
    }

    // Table + column names
    private static final class ItemsTable {
        private static final String TABLE = "items";
        private static final String COL_ID = "_id";
        private static final String COL_NAME = "name";
        private static final String COL_UPC = "upc";
        private static final String COL_SKU = "sku";
        private static final String COL_SHORT_DESC = "short_description";
        private static final String COL_QTY = "quantity";
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Make the items table
        String sql = "CREATE TABLE " + ItemsTable.TABLE + " (" +
                ItemsTable.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + // auto id
                ItemsTable.COL_NAME + " TEXT NOT NULL, " +                   // product name
                ItemsTable.COL_UPC + " TEXT NOT NULL UNIQUE, " +             // unique upc
                ItemsTable.COL_SKU + " TEXT NOT NULL UNIQUE, " +             // unique sku
                ItemsTable.COL_SHORT_DESC + " TEXT, " +                      // short description
                ItemsTable.COL_QTY + " INTEGER NOT NULL DEFAULT 0 CHECK(" + // >= 0 only
                ItemsTable.COL_QTY + " >= 0)" +
                ")";
        db.execSQL(sql);

        // Speed up name searches
        db.execSQL("CREATE INDEX idx_items_name ON " + ItemsTable.TABLE +
                "(" + ItemsTable.COL_NAME + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop and recreate table if the version changes
        db.execSQL("DROP TABLE IF EXISTS " + ItemsTable.TABLE);
        onCreate(db);
    }

    // ---------------- CRUD ----------------

    /**
     * Add one item.
     * @return row id (>0) if it worked, or -1 if it failed (like duplicate upc/sku)
     */
    public long createItem(String name, String upc, String sku,
                           @Nullable String shortDescription, int quantity) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(ItemsTable.COL_NAME, safe(name));
        cv.put(ItemsTable.COL_UPC, safe(upc));
        cv.put(ItemsTable.COL_SKU, safe(sku));
        if (shortDescription != null) cv.put(ItemsTable.COL_SHORT_DESC, shortDescription.trim());
        cv.put(ItemsTable.COL_QTY, Math.max(0, quantity)); // never below 0
        return db.insert(ItemsTable.TABLE, null, cv);
    }

    // Check if a sku already exists.
    public boolean itemExistsBySku(String sku) {
        SQLiteDatabase db = getReadableDatabase();
        String[] cols = { ItemsTable.COL_ID };
        String sel = ItemsTable.COL_SKU + " = ?";
        String[] args = { safe(sku) };
        try (Cursor c = db.query(ItemsTable.TABLE, cols, sel, args, null, null, null)) {
            return c.moveToFirst();
        }
    }

    // Check if a upc already exists
    public boolean itemExistsByUpc(String upc) {
        SQLiteDatabase db = getReadableDatabase();
        String[] cols = { ItemsTable.COL_ID };
        String sel = ItemsTable.COL_UPC + " = ?";
        String[] args = { safe(upc) };
        try (Cursor c = db.query(ItemsTable.TABLE, cols, sel, args, null, null, null)) {
            return c.moveToFirst();
        }
    }

    // Get one item by sku
    public Cursor getItemBySku(String sku) {
        SQLiteDatabase db = getReadableDatabase();
        String[] cols = {
                ItemsTable.COL_ID, ItemsTable.COL_NAME, ItemsTable.COL_UPC,
                ItemsTable.COL_SKU, ItemsTable.COL_SHORT_DESC, ItemsTable.COL_QTY
        };
        String sel = ItemsTable.COL_SKU + " = ?";
        String[] args = { safe(sku) };
        return db.query(ItemsTable.TABLE, cols, sel, args, null, null, null);
    }



    // Get all items, sorted by name
    public Cursor listAllItems() {
        SQLiteDatabase db = getReadableDatabase();
        String[] cols = {
                ItemsTable.COL_ID, ItemsTable.COL_NAME, ItemsTable.COL_UPC,
                ItemsTable.COL_SKU, ItemsTable.COL_SHORT_DESC, ItemsTable.COL_QTY
        };
        return db.query(ItemsTable.TABLE, cols, null, null, null, null,
                ItemsTable.COL_NAME + " ASC");
    }

    // ---------------- Search ----------------

    // Escape %, _ and \ so LIKE searches don’t break.
    private static String escapeLike(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    // Find items by name, case-insensitive
    public Cursor listItemsByName(String query) {
        SQLiteDatabase db = getReadableDatabase();
        String q = "%" + escapeLike(query) + "%";
        return db.query(
                ItemsTable.TABLE,
                new String[] {
                        ItemsTable.COL_ID, ItemsTable.COL_NAME, ItemsTable.COL_UPC,
                        ItemsTable.COL_SKU, ItemsTable.COL_SHORT_DESC, ItemsTable.COL_QTY
                },
                ItemsTable.COL_NAME + " LIKE ? ESCAPE '\\'",
                new String[] { q },
                null, null,
                ItemsTable.COL_NAME + " COLLATE NOCASE ASC"
        );
    }

    // ---------------- Quantity & updates ----------------

    // Set quantity by sku
    public int updateQuantityBySku(String sku, int newQuantity) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(ItemsTable.COL_QTY, Math.max(0, newQuantity));
        String where = ItemsTable.COL_SKU + " = ?";
        String[] args = { safe(sku) };
        return db.update(ItemsTable.TABLE, cv, where, args);
    }

    // Add or subtract from quantity by sku
    public int adjustQuantityBySku(String sku, int delta) {
        int updated = 0;
        try (Cursor c = getItemBySku(sku)) {
            if (c != null && c.moveToFirst()) {
                int current = c.getInt(c.getColumnIndexOrThrow(ItemsTable.COL_QTY));
                updated = updateQuantityBySku(sku, Math.max(0, current + delta));
            }
        }
        return updated;
    }

    // ---------------- Deletes ----------------

    // Delete one row by id
    public int deleteItemById(long id) {
        SQLiteDatabase db = getWritableDatabase();
        String where = ItemsTable.COL_ID + " = ?";
        String[] args = { String.valueOf(id) };
        return db.delete(ItemsTable.TABLE, where, args);
    }

    // Delete one row by sku
    public int deleteBySku(String sku) {
        SQLiteDatabase db = getWritableDatabase();
        String where = ItemsTable.COL_SKU + " = ?";
        String[] args = { safe(sku) };
        return db.delete(ItemsTable.TABLE, where, args);
    }

    // Get only items with qty == 0
    public Cursor listItemsWithZeroQty() {
        SQLiteDatabase db = getReadableDatabase();
        String[] cols = {
                "_id", "name", "upc", "sku", "short_description", "quantity"
        };
        String sel = "quantity = 0";
        return db.query("items", cols, sel, null, null, null, "name COLLATE NOCASE ASC");
    }

    // ---------------- Helpers ----------------

    //Trim strings and avoid nulls.
    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
