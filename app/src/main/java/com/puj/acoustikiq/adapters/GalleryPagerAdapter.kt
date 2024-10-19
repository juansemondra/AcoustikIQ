package com.puj.acoustikiq.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.puj.acoustikiq.fragments.CamaraFragment
import com.puj.acoustikiq.fragments.PhoneGalleryFragment

class GalleryPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CamaraFragment()
            1 -> PhoneGalleryFragment()
            else -> throw IllegalArgumentException("Fragment no válido")
        }
    }
}