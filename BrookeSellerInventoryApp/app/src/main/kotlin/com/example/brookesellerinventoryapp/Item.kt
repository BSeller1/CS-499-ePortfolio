package com.example.brookesellerinventoryapp

// item class
class Item(
    @JvmField val id: Long,
    @JvmField val name: String?,
// can be null
    @JvmField val imageUrlOrPath: String?,
    @JvmField val sku: String?,
    @JvmField val quantity: Int,
    // can be null
    @JvmField val upc: String?,
    // can be null
    @JvmField var description: String?
) {
    init {
        this.description = description
    }
}
