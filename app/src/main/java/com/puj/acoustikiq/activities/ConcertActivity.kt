package com.puj.acoustikiq.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.puj.acoustikiq.adapters.ConcertAdapter
import com.puj.acoustikiq.databinding.ActivityConcertBinding
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.util.Alerts
import java.io.Serializable

class ConcertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConcertBinding
    private lateinit var concertAdapter: ConcertAdapter
    private var concertList: MutableList<Concert> = mutableListOf()
    private lateinit var database: DatabaseReference
    private lateinit var userId: String
    private val alerts = Alerts(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConcertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = FirebaseAuth.getInstance().currentUser?.uid ?: throw Exception("Usuario no autenticado")
        database = FirebaseDatabase.getInstance().getReference("concerts/users/$userId")

        setupRecyclerView()
        loadConcertsFromFirebase()

        binding.backButton.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        binding.createConcertButton.setOnClickListener {
            startActivity(Intent(this, CreateConcertActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        concertAdapter = ConcertAdapter(concertList, ::onConcertClick)
        binding.concertRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.concertRecyclerView.adapter = concertAdapter
    }

    private fun loadConcertsFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                concertList.clear()
                for (concertSnapshot in snapshot.children) {
                    val concert = concertSnapshot.getValue(Concert::class.java)
                    concert?.let {
                        it.id = concertSnapshot.key ?: ""
                        concertList.add(it)
                    }
                }
                concertAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                alerts.showErrorDialog("Error al cargar conciertos", error.message)
            }
        })
    }

    private fun onConcertClick(concert: Concert) {
        AlertDialog.Builder(this)
            .setTitle("Opciones de Concierto")
            .setMessage("Elija una opción para el concierto ${concert.name}")
            .setPositiveButton("Editar") { _, _ ->
                val editIntent = Intent(this, EditConcertActivity::class.java)
                editIntent.putExtra("concert", concert)
                startActivity(editIntent)
            }
            .setNegativeButton("Ver Venues") { _, _ ->
                val venueIntent = Intent(this, VenueActivity::class.java)
                venueIntent.putExtra("concert", concert)
                startActivity(venueIntent)
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }
}