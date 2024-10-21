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
import com.puj.acoustikiq.R
import com.puj.acoustikiq.adapters.DateTypeAdapter
import com.puj.acoustikiq.adapters.LocationAdapter
import com.puj.acoustikiq.databinding.ActivityCreateConcertBinding
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.util.Alerts
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class CreateConcertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateConcertBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private val concertsFileName = "concerts.json"
    private val REQUEST_LOCATION_PERMISSION = 1001
    private val alerts = Alerts(this)
    private val PERM_LOCATION_CODE = 303


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

        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                alerts.indefiniteSnackbar(
                    binding.root,
                    "El permiso de Localización es necesario para usar esta actividad."
                )
            }

            else -> {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERM_LOCATION_CODE
                )
            }
        }

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

    private fun saveConcert() {
        val name = binding.concertNameEditText.text.toString()
        val dateStr = binding.concertDateEditText.text.toString()

        if (name.isEmpty() || dateStr.isEmpty() || currentLocation == null) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr)

        val newConcert = date?.let { Concert(name, it, currentLocation!!, emptyList()) }

        val concerts = loadConcertsFromFile().toMutableList()

        if (newConcert != null) {
            concerts.add(newConcert)
        }

        writeConcertsToFile(concerts)
        Toast.makeText(this, "Concierto creado exitosamente", Toast.LENGTH_SHORT).show()

        finish()
    }

    private fun loadConcertsFromFile(): List<Concert> {
        return try {
            val file = File(filesDir, concertsFileName)

            if (!file.exists()) {
                throw Exception("El archivo concerts.json no existe en el almacenamiento interno.")
            }

            val inputStream = file.inputStream()
            val reader = InputStreamReader(inputStream)

            val gson = GsonBuilder()
                .registerTypeAdapter(Location::class.java, LocationAdapter())
                .registerTypeAdapter(Date::class.java, DateTypeAdapter())
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
                .registerTypeAdapter(Date::class.java, DateTypeAdapter())
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
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}