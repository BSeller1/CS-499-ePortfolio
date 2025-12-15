package com.example.brookesellerinventoryapp;

// item class
public class Item {
    public final long   id;
    public final String name;
    public final String imageUrlOrPath;   // can be null
    public final String sku;
    public final int    quantity;
    public final String upc;              // can be null
    public final String description;      // can be null

    public Item(long id,
                String name,
                String imageUrlOrPath,
                String sku,
                int quantity,
                String upc,
                String description) {
        this.id = id;
        this.name = name;
        this.imageUrlOrPath = imageUrlOrPath;
        this.sku = sku;
        this.quantity = quantity;
        this.upc = upc;
        this.description = description;
    }
}
