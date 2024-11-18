package com.puj.acoustikiq.fragments

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import com.puj.acoustikiq.R
import com.puj.acoustikiq.databinding.FragmentRouteBinding
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.services.SharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class RouteFragment : Fragment() {

    private lateinit var binding: FragmentRouteBinding
    private lateinit var gMap: GoogleMap
    private lateinit var mapMarker: Marker
    private var position: LatLng = LatLng(-34.0, 151.0) // Default position
    private val viewModel: SharedViewModel by activityViewModels()

    private var selectedConcertLocation: LatLng? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRouteBinding.inflate(inflater, container, false)

        viewModel.concerts.observe(viewLifecycleOwner, Observer { concertsMap ->
            setupConcertSpinner(concertsMap)
        })

        val callback = OnMapReadyCallback { googleMap ->
            gMap = googleMap
            gMap.uiSettings.isZoomControlsEnabled = true
            gMap.uiSettings.isCompassEnabled = true

            mapMarker = gMap.addMarker(
                MarkerOptions().position(position).title("Ubicación Inicial")
            )!!

            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(callback)

        binding.navigateButton.setOnClickListener {
            selectedConcertLocation?.let { destination ->
                drawRoute(position, destination)
            }
        }

        return binding.root
    }

    private fun setupConcertSpinner(concertsMap: HashMap<String, Concert>) {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val concertEntries = concertsMap.entries.toList()
        val concertNames = concertEntries.map { "${it.value.name} - ${dateFormat.format(it.value.date)}" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, concertNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.concertSpinner.adapter = adapter

        binding.concertSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedConcert = concertEntries[position].value
                selectedConcertLocation = LatLng(selectedConcert.location.latitude, selectedConcert.location.longitude)
                moveCameraToConcert(selectedConcertLocation!!)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun moveCameraToConcert(location: LatLng) {
        mapMarker.position = location
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }

    fun movePerson(location: Location) {
        position = LatLng(location.latitude, location.longitude)
        mapMarker.position = position
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
    }

    private fun drawRoute(start: LatLng, destination: LatLng) {
        val apiKey = getString(R.string.google_maps_key)
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${start.latitude},${start.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&key=$apiKey"

        CoroutineScope(Dispatchers.IO).launch {
            val response = URL(url).readText()
            val jsonObject = JSONObject(response)
            val routes = jsonObject.getJSONArray("routes")

            if (routes.length() > 0) {
                val overviewPolyline = routes.getJSONObject(0).getJSONObject("overview_polyline")
                val points = overviewPolyline.getString("points")
                val decodedPath = PolyUtil.decode(points)

                CoroutineScope(Dispatchers.Main).launch {
                    gMap.addPolyline(
                        PolylineOptions()
                            .addAll(decodedPath)
                            .color(ContextCompat.getColor(requireContext(), R.color.routeColor))
                            .width(10f)
                    )
                }
            }
        }
    }
}