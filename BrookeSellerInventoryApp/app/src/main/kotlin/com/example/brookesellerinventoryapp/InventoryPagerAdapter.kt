package com.example.brookesellerinventoryapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class InventoryPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = 2 // TODO: Update with the actual number of tabs

    override fun createFragment(position: Int): Fragment {
        // TODO: Return the appropriate fragment for each tab
        return when (position) {
            0 -> InventoryFragment()
            1 -> SomethingElseFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}
