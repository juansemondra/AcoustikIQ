package com.puj.acoustikiq.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.puj.acoustikiq.adapters.VenueAdapter
import com.puj.acoustikiq.databinding.ActivityVenueBinding
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.model.LineArray
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

        setupRecyclerView()
        loadVenues()

        binding.backButton.setOnClickListener {
            val backIntent = Intent(this, ConcertActivity::class.java)
            startActivity(backIntent)
        }

        binding.createVenueButton.setOnClickListener {
            val venueIntent = Intent(this, CreateVenueActivity::class.java)
            venueIntent.putExtra("concert", concert)
            startActivity(venueIntent)
        }
    }

    private fun setupRecyclerView() {
        venueAdapter = VenueAdapter(mapOf(), ::onVenueClick)
        binding.venueRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.venueRecyclerView.adapter = venueAdapter
    }

    private fun loadVenues() {
        database.child(concert.id).child("venues").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val venues = hashMapOf<String, Venue>()

                for (venueSnapshot in snapshot.children) {
                    val venueMap = venueSnapshot.value as? HashMap<*, *>
                    if (venueMap != null) {
                        val venue = Gson().fromJson(Gson().toJson(venueMap), Venue::class.java)
                        venue.id = venueSnapshot.key ?: ""

                        if (venueMap["venueLineArray"] is HashMap<*, *>) {
                            val lineArrayMap = venueMap["venueLineArray"] as HashMap<*, *>
                            val lineArrays = hashMapOf<String, LineArray>()
                            for ((key, value) in lineArrayMap) {
                                val lineArray = Gson().fromJson(
                                    Gson().toJson(value),
                                    LineArray::class.java
                                )
                                lineArrays[key as String] = lineArray
                            }
                            venue.venueLineArray = lineArrays
                        }

                        venues[venue.id] = venue
                    }
                }

                venueAdapter.updateVenues(venues)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@VenueActivity, "Error al cargar venues: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun onVenueClick(venue: Venue) {
        AlertDialog.Builder(this)
            .setTitle("Opciones de Venue")
            .setMessage("Elija una opción para el venue ${venue.name}")
            .setPositiveButton("Editar") { _, _ ->
                val editIntent = Intent(this, EditVenueActivity::class.java)
                editIntent.putExtra("venue", venue)
                editIntent.putExtra("concert", concert as Parcelable)
                startActivity(editIntent)
            }
            .setNegativeButton("Abrir") { _, _ ->
                val openIntent = Intent(this, OpenVenueActivity::class.java)
                openIntent.putExtra("venue", venue)
                openIntent.putExtra("concert", concert as Parcelable)
                startActivity(openIntent)
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }
}