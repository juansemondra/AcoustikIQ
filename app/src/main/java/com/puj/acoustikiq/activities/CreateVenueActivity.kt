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
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.puj.acoustikiq.adapters.DateTypeAdapter
import com.puj.acoustikiq.adapters.LocationAdapter
import com.puj.acoustikiq.databinding.ActivityCreateVenueBinding
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.model.Venue
import com.puj.acoustikiq.util.Alerts
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class CreateVenueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateVenueBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private val venuesFileName = "venues.json"
    private val concertsFileName = "concerts.json"
    private val REQUEST_LOCATION_PERMISSION = 1001
    private val alerts = Alerts(this)
    private val PERM_LOCATION_CODE = 303
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
            saveVenue()
        }
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
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

    @SuppressLint("SetTextI18n")
    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    binding.locationTextView.text =
                        "Latitud: ${location.latitude}, Longitud: ${location.longitude}, Altitud: ${location.altitude}"
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error obteniendo la ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveVenue() {
        val venueName = binding.venueNameEditText.text.toString()
        val temperature = binding.venueTemperatureEditText.text.toString().toDoubleOrNull()

        if (venueName.isEmpty() || temperature == null || currentLocation == null) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val newVenue = Venue(
            name = venueName,
            venueLineArray = mutableListOf(),
            temperature = temperature
        )

        val venues = loadVenuesFromFile().toMutableList()
        venues.add(newVenue)
        writeVenuesToFile(venues)

        updateConcertWithVenue(newVenue)

        Toast.makeText(this, "Venue creado exitosamente", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun loadVenuesFromFile(): List<Venue> {
        return try {
            val file = File(filesDir, venuesFileName)
            if (!file.exists()) {
                return emptyList()
            }
            val inputStream = file.inputStream()
            val reader = InputStreamReader(inputStream)
            val gson = GsonBuilder()
                .registerTypeAdapter(Location::class.java, LocationAdapter())
                .create()

            val venueType = object : TypeToken<List<Venue>>() {}.type
            gson.fromJson(reader, venueType)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun writeVenuesToFile(venues: List<Venue>) {
        try {
            val file = File(filesDir, venuesFileName)
            val outputStream = file.outputStream()
            val writer = OutputStreamWriter(outputStream)
            val gson = GsonBuilder()
                .registerTypeAdapter(Location::class.java, LocationAdapter())
                .create()

            gson.toJson(venues, writer)
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateConcertWithVenue(newVenue: Venue) {
        val concerts = loadConcertsFromFile().toMutableList()
        val selectedConcert = concerts.find { it.name == concert.name }

        if (selectedConcert != null) {
            selectedConcert.venues = selectedConcert.venues.toMutableList().apply {
                add(newVenue)
            }
            writeConcertsToFile(concerts)
        } else {
            Toast.makeText(this, "Concierto no encontrado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadConcertsFromFile(): List<Concert> {
        return try {
            val file = File(filesDir, concertsFileName)
            if (!file.exists()) {
                return emptyList()
            }
            val inputStream = file.inputStream()
            val reader = InputStreamReader(inputStream)
            val gson = GsonBuilder()
                .registerTypeAdapter(Location::class.java, LocationAdapter())
                .registerTypeAdapter(java.util.Date::class.java, DateTypeAdapter())
                .create()

            val concertType = object : TypeToken<List<Concert>>() {}.type
            gson.fromJson(reader, concertType)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun writeConcertsToFile(concerts: List<Concert>) {
        try {
            val file = File(filesDir, concertsFileName)
            val outputStream = file.outputStream()
            val writer = OutputStreamWriter(outputStream)
            val gson = GsonBuilder()
                .registerTypeAdapter(Location::class.java, LocationAdapter())
                .registerTypeAdapter(java.util.Date::class.java, DateTypeAdapter())
                .create()

            gson.toJson(concerts, writer)
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
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