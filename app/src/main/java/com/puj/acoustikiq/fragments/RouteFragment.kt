package com.puj.acoustikiq.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.puj.acoustikiq.R
import com.puj.acoustikiq.adapters.DateTypeAdapter
import com.puj.acoustikiq.adapters.LocationAdapter
import com.puj.acoustikiq.databinding.FragmentRouteBinding
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.util.Alerts
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class RouteFragment : Fragment(), SensorEventListener {
    private lateinit var binding: FragmentRouteBinding
    private lateinit var alerts: Alerts
    lateinit var gMap: GoogleMap
    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private var zoomLevel = 15f
    private lateinit var mapMarker: Marker
    private lateinit var polylineOptions: PolylineOptions
    private var concerts: List<Concert> = listOf()
    private var selectedConcertLocation: LatLng? = null

    private var position: LatLng = LatLng(-34.0, 151.0)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRouteBinding.inflate(inflater, container, false)
        polylineOptions = PolylineOptions().width(5f).color(Color.RED).geodesic(true)

        concerts = loadConcertsFromJSON()

        setupConcertSpinner()

        val callback = OnMapReadyCallback { googleMap ->
            gMap = googleMap
            gMap.uiSettings.isZoomControlsEnabled = true
            gMap.uiSettings.isCompassEnabled = true

            mapMarker = gMap.addMarker(
                MarkerOptions().position(position).title("Ubicación Inicial")
                    .icon(context?.let { bitmapDescriptorFromVector(it, R.drawable.person_24px) })
            )!!

            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoomLevel))
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(callback)

        sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!

        binding.navigateButton.setOnClickListener {
            selectedConcertLocation?.let { location ->
                openGoogleMapsForDirections(location)
            }
        }

        return binding.root
    }

    private fun setupConcertSpinner() {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val concertNames = concerts.map { "${it.name} - ${dateFormat.format(it.date)}" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, concertNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.concertSpinner.adapter = adapter

        binding.concertSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedConcert = concerts[position]
                selectedConcertLocation = LatLng(selectedConcert.location.latitude, selectedConcert.location.longitude)
                moveCameraToConcert(selectedConcertLocation!!)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun openGoogleMapsForDirections(destination: LatLng) {
        val uri = Uri.parse("google.navigation:q=${destination.latitude},${destination.longitude}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            alerts.shortToast("Google Maps no está instalado.")
        }
    }

    private fun moveCameraToConcert(location: LatLng) {
        mapMarker.position = location
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel))
    }

    private fun loadConcertsFromJSON(): List<Concert> {
        val concerts = mutableListOf<Concert>()
        try {
            val gson = GsonBuilder()
                .registerTypeAdapter(Location::class.java, LocationAdapter())
                .registerTypeAdapter(Date::class.java, DateTypeAdapter())
                .create()

            val inputStream = requireContext().assets.open("concerts.json")
            val reader = InputStreamReader(inputStream)

            val concertType = object : TypeToken<List<Concert>>() {}.type
            concerts.addAll(gson.fromJson(reader, concertType))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return concerts
    }

    fun movePerson(location: Location) {
        position = LatLng(location.latitude, location.longitude)
        mapMarker.position = position
        mapMarker.zIndex = 10.0f

        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, zoomLevel))

        drawPolyline(location)
    }

    fun drawPolyline(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        polylineOptions.add(latLng)
        gMap.addPolyline(polylineOptions)
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable?.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(
            vectorDrawable!!.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (this::gMap.isInitialized) {
            if (event!!.values[0] > 80) {
                gMap.setMapStyle(
                    context?.let { MapStyleOptions.loadRawResourceStyle(it, R.raw.map_day) }
                )
            } else {
                gMap.setMapStyle(
                    context?.let { MapStyleOptions.loadRawResourceStyle(it, R.raw.map_night) }
                )
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
}