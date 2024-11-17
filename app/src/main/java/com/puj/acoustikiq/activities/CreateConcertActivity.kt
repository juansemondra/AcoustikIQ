package com.puj.acoustikiq.activities

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.puj.acoustikiq.databinding.ActivityCreateConcertBinding
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.model.Position
import java.text.SimpleDateFormat
import java.util.*

class CreateConcertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateConcertBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private val REQUEST_LOCATION_PERMISSION = 1001

    private val auth = FirebaseAuth.getInstance()
    private val database: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("concerts/users/${auth.currentUser?.uid}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateConcertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLocationPermission()

        binding.saveButton.setOnClickListener {
            saveConcert()
        }
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            currentLocation = location
            if (location != null) {
                binding.locationTextView.text = "Lat: ${location.latitude}, Lng: ${location.longitude}"
            } else {
                Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveConcert() {
        val name = binding.concertNameEditText.text.toString()
        val dateStr = binding.concertDateEditText.text.toString()

        if (name.isEmpty() || dateStr.isEmpty() || currentLocation == null) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr)?.time ?: return

        val uniqueId = database.push().key ?: UUID.randomUUID().toString()
        val newConcert = Concert(
            id = uniqueId,
            name = name,
            date = date,
            location = Position(
                latitude = currentLocation!!.latitude,
                longitude = currentLocation!!.longitude,
                isOnline = false
            ),
            venues = hashMapOf()
        )

        database.child(uniqueId).setValue(newConcert)
            .addOnSuccessListener {
                Toast.makeText(this, "Concierto creado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar el concierto", Toast.LENGTH_LONG).show()
            }
    }
}