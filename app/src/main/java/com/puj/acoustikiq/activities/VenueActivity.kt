package com.puj.acoustikiq.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.puj.acoustikiq.adapters.VenueAdapter
import com.puj.acoustikiq.databinding.ActivityVenueBinding
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.model.Venue

class VenueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVenueBinding
    private lateinit var concert: Concert
    private lateinit var venueAdapter: VenueAdapter

    private val auth = FirebaseAuth.getInstance()
    private val database: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("concerts/users/${auth.currentUser?.uid}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVenueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        concert = intent.getParcelableExtra("concert")
            ?: throw NullPointerException("Concert object is missing in intent")

        loadVenues()

        binding.backButton.setOnClickListener {
            val backIntent = Intent(this, ConcertActivity::class.java)
            startActivity(backIntent)
        }

        binding.createVenueButton.setOnClickListener {
            val venueIntent = Intent(this, CreateVenueActivity::class.java)
            venueIntent.putExtra("concertId", concert.id)
            startActivity(venueIntent)
        }
    }

    private fun loadVenues() {
        database.child(concert.id).child("venues").get()
            .addOnSuccessListener { snapshot ->
                val venues = snapshot.children.mapNotNull { it.getValue(Venue::class.java) }
                setupVenueAdapter(venues)
            }
            .addOnFailureListener { error ->
                // Manejo de error
            }
    }

    private fun setupVenueAdapter(venues: List<Venue>) {
        venueAdapter = VenueAdapter(venues, ::onVenueClick)
        binding.venueRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.venueRecyclerView.adapter = venueAdapter
    }

    private fun onVenueClick(venue: Venue) {
        AlertDialog.Builder(this)
            .setTitle("Opciones de Venue")
            .setMessage("Elija una opción para el venue ${venue.name}")
            .setPositiveButton("Editar") { _, _ ->
                val editIntent = Intent(this, EditVenueActivity::class.java)
                editIntent.putExtra("venueId", venue.id)
                editIntent.putExtra("concertId", concert.id)
                startActivity(editIntent)
            }
            .setNegativeButton("Abrir") { _, _ ->
                val openIntent = Intent(this, OpenVenueActivity::class.java)
                openIntent.putExtra("venueId", venue.id)
                openIntent.putExtra("concertId", concert.id)
                startActivity(openIntent)
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }
}