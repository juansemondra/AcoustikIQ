package com.puj.acoustikiq.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.puj.acoustikiq.databinding.ActivityGalleryBinding
import com.puj.acoustikiq.adapters.GalleryPagerAdapter

class GalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGalleryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewPager = binding.viewPager
        val adapter = GalleryPagerAdapter(this)
        viewPager.adapter = adapter

        val tabLayout = binding.tabLayout
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Cámara"
                1 -> "Galería"
                else -> null
            }
        }.attach()

        binding.backButton.setOnClickListener(){
            val backIntent = Intent(this, MenuActivity::class.java)
            startActivity(backIntent)
        }

    }
}