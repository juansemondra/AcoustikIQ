package com.puj.acoustikiq.activities

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.puj.acoustikiq.R
import com.puj.acoustikiq.databinding.ActivityRouteBinding
import com.puj.acoustikiq.fragments.RouteFragment
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.services.SharedViewModel
import com.puj.acoustikiq.util.Alerts

class RouteActivity : AppCompatActivity() {

    private val TAG = RouteActivity::class.java.name
    private lateinit var binding: ActivityRouteBinding

    private val PERM_LOCATION_CODE = 303
    private var alerts = Alerts(this)
    private lateinit var position: Location

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val viewModel: SharedViewModel by viewModels()

    private lateinit var database: DatabaseReference
    private val userId: String by lazy { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        database = FirebaseDatabase.getInstance().getReference("concerts/users/$userId")

        setupLocation()

        if (checkLocationPermission()) {
            startLocationUpdates()
            loadMapsFragment()
        } else {
            requestLocationPermission()
        }

        loadConcertsFromFirebase()
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

    private fun loadMapsFragment() {
        val existingFragment = supportFragmentManager.findFragmentById(R.id.googleMapsFragment)
        if (existingFragment == null) {
            supportFragmentManager.commit {
                replace(R.id.googleMapsFragment, RouteFragment())
            }
        }
    }

    private fun loadConcertsFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val concertsMap = hashMapOf<String, Concert>()
                for (concertSnapshot in snapshot.children) {
                    val concert = concertSnapshot.getValue(Concert::class.java)
                    concert?.let {
                        it.id = concertSnapshot.key.orEmpty()
                        concertsMap[it.id] = it
                    }
                }
                viewModel.concerts.value = concertsMap
            }

            override fun onCancelled(error: DatabaseError) {
                alerts.shortToast("Error al cargar conciertos: ${error.message}")
            }
        })
    }

    private fun setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).apply {
            setMinUpdateDistanceMeters(5F)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    val fragment = supportFragmentManager.findFragmentById(R.id.googleMapsFragment) as RouteFragment
                    fragment.movePerson(location)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (checkLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}