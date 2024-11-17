package com.puj.acoustikiq.activities

import android.Manifest
import android.annotation.SuppressLint
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
import com.google.firebase.database.FirebaseDatabase
import com.puj.acoustikiq.databinding.ActivityCreateVenueBinding
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.model.Position
import com.puj.acoustikiq.model.Venue
import java.util.*

class CreateVenueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateVenueBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private val REQUEST_LOCATION_PERMISSION = 1001
    private lateinit var concert: Concert

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateVenueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        concert = intent.getParcelableExtra("concert")
            ?: throw NullPointerException("Concert object is missing in intent")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLocationPermission()

        binding.saveButton.setOnClickListener {
            saveVenueToFirebase()
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

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                binding.locationTextView.text =
                    "Latitud: ${location.latitude}, Longitud: ${location.longitude}"
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error obteniendo la ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveVenueToFirebase() {
        val venueName = binding.venueNameEditText.text.toString()
        val temperature = binding.venueTemperatureEditText.text.toString().toDoubleOrNull()

        if (venueName.isEmpty() || temperature == null || currentLocation == null) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val venueId = UUID.randomUUID().toString()
        val venuePath = "concerts/users/$userId/${concert.id}/venues/$venueId"

        val position = Position(
            latitude = currentLocation!!.latitude,
            longitude = currentLocation!!.longitude
        )

        val newVenue = Venue(
            id = venueId,
            name = venueName,
            position = position,
            temperature = temperature,
            venueLineArray = mutableListOf()
        )

        FirebaseDatabase.getInstance().reference
            .child(venuePath)
            .setValue(newVenue)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Venue creado exitosamente", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error al guardar el venue", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Error: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}