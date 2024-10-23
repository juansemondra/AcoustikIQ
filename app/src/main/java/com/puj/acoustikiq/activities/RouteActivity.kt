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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.puj.acoustikiq.R
import com.puj.acoustikiq.adapters.DateTypeAdapter
import com.puj.acoustikiq.databinding.ActivityRouteBinding
import com.puj.acoustikiq.fragments.RouteFragment
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.util.Alerts
import com.puj.acoustikiq.services.SharedViewModel
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.Date

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

        if (checkLocationPermission()) {
            loadMapsFragment()
        } else {
            requestLocationPermission()
        }

        val concerts = loadConcerts()
        viewModel.concerts.value = concerts
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

    private fun loadConcerts(): List<Concert> {
        val concertsFile = File(filesDir, "concerts.json")

        if (!concertsFile.exists()) {
            throw Exception("El archivo concerts.json no existe en la memoria local.")
        }

        val inputStream = FileInputStream(concertsFile)
        val reader = InputStreamReader(inputStream)

        val gson = GsonBuilder()
            .registerTypeAdapter(Date::class.java, DateTypeAdapter())
            .create()

        val concertType = object : TypeToken<List<Concert>>() {}.type

        return gson.fromJson(reader, concertType)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERM_LOCATION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()
                } else {
                    alerts.shortSimpleSnackbar(
                        binding.root,
                        "Me acaban de negar los permisos de Localización 😭"
                    )
                }
            }
        }
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
                    Log.i(TAG, "onLocationResult: $location")

                    binding.animationBeer.speed = (location.speed * 3.6F) / 8F

                    val fragment = supportFragmentManager.findFragmentById(R.id.googleMapsFragment) as RouteFragment
                    fragment.movePerson(location)
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
            binding.animationBeer.resumeAnimation()
        } else {
            binding.animationBeer.pauseAnimation()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        binding.animationBeer.pauseAnimation()
    }
}