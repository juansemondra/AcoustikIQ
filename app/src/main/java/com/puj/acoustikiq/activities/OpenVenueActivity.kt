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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.puj.acoustikiq.R
import com.puj.acoustikiq.databinding.ActivityOpenVenueBinding
import com.puj.acoustikiq.fragments.MapsFragment
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.model.LineArray
import com.puj.acoustikiq.model.Venue
import com.puj.acoustikiq.util.Alerts

class OpenVenueActivity : AppCompatActivity() {

    private val TAG = OpenVenueActivity::class.java.simpleName
    private lateinit var binding: ActivityOpenVenueBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var mapsFragment: MapsFragment

    private lateinit var concert: Concert
    private lateinit var venue: Venue
    private val PERM_LOCATION_CODE = 303
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    private var alerts = Alerts(this)
    private var position: Location? = null

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
        requestLocationPermission()

        loadMapsFragment()

        binding.backButton.setOnClickListener {
            val backIntent = Intent(this, VenueActivity::class.java)
            startActivity(backIntent)
        }
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
                    position = location
                    mapsFragment.moveFunction(location)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERM_LOCATION_CODE
            )
        } else {
            startLocationUpdates()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_LOCATION_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMapsFragment() {
        mapsFragment = MapsFragment().apply {
            arguments = Bundle().apply {
                putParcelable("concert", concert)
                putParcelable("venue", venue)
            }
        }
        supportFragmentManager.commit {
            replace(R.id.map_fragment, mapsFragment)
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
}