package com.example.brookesellerinventoryapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

// Adapter that shows item cards in a RecyclerView.
public class InventoryCardAdapter extends ListAdapter<Item, InventoryCardAdapter.VH> {

    // Click events from a card.
    public interface OnItemAction {
        void onClick(Item item);         // tap a card
        default void onIncrease(Item item) {}
        default void onDecrease(Item item) {}
    }

    private final OnItemAction listener;

    public InventoryCardAdapter(@NonNull OnItemAction listener) {
        super(DIFF);
        this.listener = listener;
        setHasStableIds(true);           // stable ids help RecyclerView animations
    }

    // Tells RecyclerView how to detect changes in the list.
    private static final DiffUtil.ItemCallback<Item> DIFF = new DiffUtil.ItemCallback<>() {
        @Override public boolean areItemsTheSame(@NonNull Item a, @NonNull Item b) {
            // Same row if DB id matches; else compare sku
            if (a.id != 0 && b.id != 0) return a.id == b.id;
            if (a.sku == null || b.sku == null) return false;
            return a.sku.equals(b.sku);
        }

        @Override public boolean areContentsTheSame(@NonNull Item a, @NonNull Item b) {
            // Same content if all visible fields match
            return a.id == b.id
                    && safeEq(a.name, b.name)
                    && safeEq(a.sku, b.sku)
                    && safeEq(a.upc, b.upc)
                    && safeEq(a.description, b.description)
                    && safeEq(a.imageUrlOrPath, b.imageUrlOrPath)
                    && a.quantity == b.quantity;
        }

        private boolean safeEq(String x, String y) {
            return (x == null) ? (y == null) : x.equals(y);
        }
    };

    @Override public long getItemId(int position) {
        Item it = getItem(position);
        if (it == null) return RecyclerView.NO_ID;
        if (it.id != 0) return it.id;                    // prefer DB id
        return (it.sku != null) ? it.sku.hashCode() : RecyclerView.NO_ID;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate one card view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Item it = getItem(position);

        // Name and quantity text
        h.txtName.setText(it.name != null ? it.name : "");
        h.txtQty.setText("Qty: " + it.quantity);

        // Load image (or a placeholder)
        if (it.imageUrlOrPath == null || it.imageUrlOrPath.isEmpty()) {
            h.imgItem.setImageResource(android.R.drawable.ic_menu_report_image);
        } else {
            Glide.with(h.imgItem.getContext())
                    .load(it.imageUrlOrPath)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_menu_report_image)
                    .centerCrop()
                    .into(h.imgItem);
        }

        // Click opens details
        h.cardRoot.setOnClickListener(v -> {
            if (listener != null) listener.onClick(it);
        });

        // Long press sends a decrease event
        h.cardRoot.setOnLongClickListener(v -> {
            if (listener != null) listener.onDecrease(it);
            return true;
        });
    }

    /* ---------- ViewHolder ---------- */
    public static class VH extends RecyclerView.ViewHolder {
        final View cardRoot;          // root view of the card
        final ImageView imgItem;      // product image
        final TextView txtName;       // product name
        final TextView txtQty;        // product quantity

        public VH(@NonNull View itemView) {
            super(itemView);
            View root = itemView.findViewById(R.id.cardRoot);
            cardRoot = (root != null) ? root : itemView;
            imgItem  = itemView.findViewById(R.id.imgItem);
            txtName  = itemView.findViewById(R.id.txtName);
            txtQty   = itemView.findViewById(R.id.txtQty);
        }
    }
}
