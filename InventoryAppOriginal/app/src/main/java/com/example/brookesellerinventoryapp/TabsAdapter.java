package com.example.brookesellerinventoryapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TabsAdapter extends FragmentStateAdapter {

    public TabsAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // position 0 = Inventory tab, position 1 = Passwords tab
        return position == 0 ? new InventoryFragment() : new PasswordsFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
