package com.puj.acoustikiq.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.puj.acoustikiq.R
import com.puj.acoustikiq.fragments.MapsFragment
import com.puj.acoustikiq.model.Speaker
import com.puj.acoustikiq.model.Venue
import java.io.File
import java.io.FileReader

class OpenVenueActivity : AppCompatActivity() {

    private lateinit var venue: Venue
    private lateinit var speakersList: List<Speaker>

    private val PERM_LOCATION_CODE = 303
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_venue)

        venue = intent.getParcelableExtra("venue")
            ?: throw NullPointerException("Venue object is missing in intent")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        loadSpeakersFromJson()

        if (checkLocationPermission()) {
            loadMapsFragment()
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERM_LOCATION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_LOCATION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMapsFragment()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMapsFragment() {
        val mapsFragment = MapsFragment().apply {
            arguments = Bundle().apply {
                putParcelable("venue", venue)
            }
        }
        supportFragmentManager.commit {
            replace(R.id.map_fragment, mapsFragment)
        }
    }

    private fun loadSpeakersFromJson() {
        val speakersFile = File(filesDir, "speakers.json")
        if (!speakersFile.exists()) return

        val gson = Gson()
        val speakerType = object : TypeToken<List<Speaker>>() {}.type
        val reader = FileReader(speakersFile)
        speakersList = gson.fromJson(reader, speakerType)
        reader.close()
    }
}