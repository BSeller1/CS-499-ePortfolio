package com.example.brookesellerinventoryapp

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.brookesellerinventoryapp.InventoryCardAdapter.VH

// Adapter that shows item cards in a RecyclerView.
class InventoryCardAdapter(private val listener: OnItemAction) : ListAdapter<Item, VH?>(DIFF) {
    // Click events from a card.
    interface OnItemAction {
        fun onClick(item: Item?) // tap a card
        fun onIncrease(item: Item?) {}
        fun onDecrease(item: Item?) {}
    }

    init {
        setHasStableIds(true) // stable ids help RecyclerView animations
    }

    override fun getItemId(position: Int): Long {
        val it = getItem(position)
        if (it == null) return RecyclerView.NO_ID
        if (it.id != 0L) return it.id // prefer DB id

        return if (it.sku != null) it.sku.hashCode().toLong() else RecyclerView.NO_ID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // Inflate one card view
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val it = getItem(position)

        // Name and quantity text
        h.txtName.text = it.name ?: ""
        h.txtQty.text = "Qty: " + it.quantity

        // Load image (or a placeholder)
        if (it.imageUrlOrPath == null || it.imageUrlOrPath.isEmpty()) {
            h.imgItem.setImageResource(android.R.drawable.ic_menu_report_image)
        } else {
            Glide.with(h.imgItem.context)
                .load(it.imageUrlOrPath)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(h.imgItem)
        }

        // Click opens details
        h.cardRoot.setOnClickListener(View.OnClickListener { v: View? ->
            listener.onClick(it)
        })

        // Long press sends a decrease event
        h.cardRoot.setOnLongClickListener(OnLongClickListener { v: View? ->
            listener.onDecrease(it)
            true
        })
    }

    /* ---------- ViewHolder ---------- */
    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardRoot: View // root view of the card
        val imgItem: ImageView // product image
        val txtName: TextView // product name
        val txtQty: TextView // product quantity

        init {
            val root = itemView.findViewById<View?>(R.id.cardRoot)
            cardRoot = root ?: itemView
            imgItem = itemView.findViewById<ImageView>(R.id.imgItem)
            txtName = itemView.findViewById<TextView>(R.id.txtName)
            txtQty = itemView.findViewById<TextView>(R.id.txtQty)
        }
    }

    companion object {
        // Tells RecyclerView how to detect changes in the list.
        private val DIFF: DiffUtil.ItemCallback<Item?> = object : DiffUtil.ItemCallback<Item?>() {
            override fun areItemsTheSame(a: Item, b: Item): Boolean {
                // Same row if DB id matches; else compare sku
                if (a.id != 0L && b.id != 0L) return a.id == b.id
                if (a.sku == null || b.sku == null) return false
                return a.sku == b.sku
            }

            override fun areContentsTheSame(a: Item, b: Item): Boolean {
                // Same content if all visible fields match
                return a.id == b.id && safeEq(a.name, b.name)
                        && safeEq(a.sku, b.sku)
                        && safeEq(a.upc, b.upc)
                        && safeEq(a.description, b.description)
                        && safeEq(a.imageUrlOrPath, b.imageUrlOrPath)
                        && a.quantity == b.quantity
            }

            private fun safeEq(x: String?, y: String?): Boolean {
                return if (x == null) (y == null) else (x == y)
            }
        }
    }
}
