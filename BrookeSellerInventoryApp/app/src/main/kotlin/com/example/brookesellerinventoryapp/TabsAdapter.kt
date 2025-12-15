package com.example.brookesellerinventoryapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class TabsAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun createFragment(position: Int): Fragment {
        // position 0 = Inventory tab, position 1 = Passwords tab
        return if (position == 0) InventoryFragment() else PasswordsFragment()
    }

    override fun getItemCount(): Int {
        return 2
    }
}
