package com.puj.acoustikiq.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import com.google.android.gms.location.*
import com.puj.acoustikiq.R
import com.puj.acoustikiq.databinding.ActivityOpenVenueBinding
import com.puj.acoustikiq.fragments.MapsFragment
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.model.Speaker
import com.puj.acoustikiq.model.Venue
import com.puj.acoustikiq.util.Alerts
import java.io.File
import java.io.FileReader

class OpenVenueActivity : AppCompatActivity() {

    private val TAG = OpenVenueActivity::class.java
    private lateinit var concert: Concert
    private lateinit var venue: Venue
    private lateinit var speakersList: List<Speaker>
    private val PERM_LOCATION_CODE = 303

    private var alerts = Alerts(this)
    private lateinit var position: Location

    private lateinit var binding: ActivityOpenVenueBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var mapsFragment: MapsFragment

    private val venuesFileName = "venues.json"
    private val concertsFileName = "concerts.json"
    private val linearraysFileName = "linearrays.json"
    private val speakersFileName = "speakers.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOpenVenueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        concert = intent.getParcelableExtra("concert")
            ?: throw NullPointerException("Concert object is missing in intent")
        venue = intent.getParcelableExtra("venue")
            ?: throw NullPointerException("Venue object is missing in intent")

        setupLocation()

        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startLocationUpdates()
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

        loadSpeakersFromJson()

        if (checkLocationPermission()) {
            loadMapsFragment()
        } else {
            requestLocationPermission()
        }

        binding.backButton.setOnClickListener(){
            val backIntent = Intent(this, VenueActivity::class.java)
            startActivity(backIntent)
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
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMapsFragment() {
        val existingFragment = supportFragmentManager.findFragmentById(R.id.map_fragment)
        if (existingFragment == null) {
            mapsFragment = MapsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("concert", concert)
                    putParcelable("venue", venue)
                }
            }
            supportFragmentManager.commit {
                replace(R.id.map_fragment, mapsFragment)
            }
        } else {
            mapsFragment = existingFragment as MapsFragment
        }
    }

    private fun loadSpeakersFromJson() {
        val speakersFile = File(filesDir, speakersFileName)
        if (!speakersFile.exists()) return

        val gson = com.google.gson.Gson()
        val speakerType = object : com.google.gson.reflect.TypeToken<List<Speaker>>() {}.type
        val reader = FileReader(speakersFile)
        speakersList = gson.fromJson(reader, speakerType)
        reader.close()
    }

    private fun setupLocation() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).apply {
            setMinUpdateDistanceMeters(5F)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    Log.i(TAG.toString(), "onLocationResult: $location")
                    binding.moveAnimation.speed = (location.speed * 3.6F) / 8F

                    mapsFragment.moveFunction(location)

                    position = location
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
            binding.moveAnimation.resumeAnimation()
        } else {
            binding.moveAnimation.pauseAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        binding.moveAnimation.pauseAnimation()
    }
}