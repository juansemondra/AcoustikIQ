package com.puj.acoustikiq.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.puj.acoustikiq.adapters.VenueAdapter
import com.puj.acoustikiq.databinding.ActivityVenueBinding
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.model.Venue

class VenueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVenueBinding
    private lateinit var concert: Concert
    private lateinit var venueAdapter: VenueAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVenueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        concert = intent.getParcelableExtra("concert")
            ?: throw NullPointerException("Concert object is missing in intent")

        venueAdapter = VenueAdapter(concert.venues, ::onVenueClick)
        binding.venueRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.venueRecyclerView.adapter = venueAdapter
    }

    private fun onVenueClick(venue: Venue) {
        AlertDialog.Builder(this)
            .setTitle("Opciones de Venue")
            .setMessage("Elija una opción para el venue ${venue.name}")
            .setPositiveButton("Editar") { _, _ ->
                val editIntent = Intent(this, EditVenueActivity::class.java)
                editIntent.putExtra("venue", venue)
                startActivity(editIntent)
            }
            .setNegativeButton("Abrir") { _, _ ->
                val openIntent = Intent(this, OpenVenueActivity::class.java)
                openIntent.putExtra("venue", venue)
                startActivity(openIntent)
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }
}