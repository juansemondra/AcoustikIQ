package com.puj.acoustikiq.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.puj.acoustikiq.R
import com.puj.acoustikiq.databinding.FragmentMapsBinding
import com.puj.acoustikiq.databinding.DialogAddLineArrayBinding
import com.puj.acoustikiq.model.LineArray
import com.puj.acoustikiq.model.Speaker
import com.puj.acoustikiq.model.Venue
import com.puj.acoustikiq.adapters.SpeakerAdapter
import com.puj.acoustikiq.model.Concert
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class MapsFragment : Fragment(), SensorEventListener {

    private lateinit var binding: FragmentMapsBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var rotationSensor: Sensor
    private lateinit var gMap: GoogleMap
    private lateinit var venue: Venue
    private var speakersList: List<Speaker> = listOf()
    private var currentRotation = 0f

    private var position: LatLng = LatLng(-34.0, 151.0)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapsBinding.inflate(inflater, container, false)

        venue = arguments?.getParcelable("venue") ?: throw IllegalStateException("Venue not found in arguments")

        if (!::venue.isInitialized) {
            throw IllegalStateException("Venue is not initialized")
        }

        sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)!!

        loadSpeakersFromJson()

        val callback = OnMapReadyCallback { googleMap ->
            gMap = googleMap
            gMap.uiSettings.isZoomControlsEnabled = false

            venue.venueLineArray.forEach { lineArray ->
                val location = LatLng(lineArray.location.latitude, lineArray.location.longitude)
                val marker = gMap.addMarker(
                    MarkerOptions().position(location).title(lineArray.system.model)
                        .icon(
                            bitmapDescriptorFromVector(
                                requireContext(),
                                R.drawable.military_tech_24px
                            )
                        )
                )
                marker?.tag = lineArray
            }

            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))

            gMap.setOnMarkerClickListener { marker ->
                val lineArray = marker.tag as? LineArray
                lineArray?.let {
                    showEditLineArrayDialog(it, marker)
                }
                true
            }
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(callback)

        binding.addLineArrayButton.setOnClickListener {
            showAddLineArrayDialog()
        }

        return binding.root
    }

    private fun showEditLineArrayDialog(lineArray: LineArray, marker: Marker) {
        val dialogBinding = DialogAddLineArrayBinding.inflate(LayoutInflater.from(context))

        dialogBinding.speakerDropdown.setSelection(speakersList.indexOf(lineArray.system))
        dialogBinding.quantityInput.setText(lineArray.quantity.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Editar Line Array")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                lineArray.system = dialogBinding.speakerDropdown.selectedItem as Speaker
                lineArray.quantity = dialogBinding.quantityInput.text.toString().toIntOrNull() ?: lineArray.quantity

                marker.position = LatLng(lineArray.location.latitude, lineArray.location.longitude)
                marker.title = lineArray.system.model

                saveLineArrayToFiles(lineArray)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun loadSpeakersFromJson() {
        val speakersFile = File(requireContext().filesDir, "speakers.json")
        if (!speakersFile.exists()) return

        val gson = Gson()
        val speakerType = object : TypeToken<List<Speaker>>() {}.type
        val reader = FileReader(speakersFile)
        speakersList = gson.fromJson(reader, speakerType)
        reader.close()
    }

    private fun showAddLineArrayDialog() {
        val dialogBinding = DialogAddLineArrayBinding.inflate(LayoutInflater.from(context))

        val speakerAdapter = SpeakerAdapter(requireContext(), android.R.layout.simple_spinner_item, speakersList)
        dialogBinding.speakerDropdown.adapter = speakerAdapter

        AlertDialog.Builder(requireContext())
            .setTitle("Agregar Line Array")
            .setView(dialogBinding.root)
            .setPositiveButton("Crear") { _, _ ->
                val selectedSpeaker = dialogBinding.speakerDropdown.selectedItem as Speaker
                val quantity = dialogBinding.quantityInput.text.toString().toIntOrNull() ?: 0

                val newLineArray = LineArray(
                    type = selectedSpeaker.model,
                    system = selectedSpeaker,
                    quantity = quantity,
                    location = getCurrentLocation(),
                    delay = 0f,
                    calibratedFreq = false,
                    calibratedPhase = false
                )

                venue.venueLineArray.add(newLineArray)
                saveLineArrayToFiles(newLineArray)

                val markerOptions = MarkerOptions()
                    .position(LatLng(getCurrentLocation().latitude, getCurrentLocation().longitude))
                    .title(newLineArray.type)
                    .rotation(currentRotation)
                    .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.military_tech_24px))

                gMap.addMarker(markerOptions)

                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(getCurrentLocation().latitude, getCurrentLocation().longitude), 15f))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveLineArrayToFiles(lineArray: LineArray) {
        saveLineArrayToFile(lineArray)

        val venuesFile = File(requireContext().filesDir, "venues.json")
        updateVenueInFile(venue, venuesFile)

        val concertsFile = File(requireContext().filesDir, "concerts.json")
        updateConcertInFile(venue, concertsFile)
    }

    private fun updateVenueInFile(updatedVenue: Venue, file: File) {
        val gson = Gson()
        val venuesList: MutableList<Venue> = if (file.exists()) {
            val reader = FileReader(file)
            val type = object : TypeToken<MutableList<Venue>>() {}.type
            val existingList: MutableList<Venue> = gson.fromJson(reader, type)
            reader.close()
            existingList
        } else {
            mutableListOf()
        }

        val venueIndex = venuesList.indexOfFirst { it.name == updatedVenue.name }
        if (venueIndex != -1) {
            venuesList[venueIndex] = updatedVenue
        } else {
            venuesList.add(updatedVenue)
        }

        val writer = FileWriter(file)
        writer.write(gson.toJson(venuesList))
        writer.close()
    }

    private fun updateConcertInFile(updatedVenue: Venue, file: File) {
        val gson = Gson()
        val concertsList: MutableList<Concert> = if (file.exists()) {
            val reader = FileReader(file)
            val type = object : TypeToken<MutableList<Concert>>() {}.type
            val existingList: MutableList<Concert> = gson.fromJson(reader, type)
            reader.close()
            existingList
        } else {
            mutableListOf()
        }

        concertsList.forEach { concert ->
            concert.venues.find { it.name == updatedVenue.name }?.let { foundVenue ->
                foundVenue.venueLineArray = updatedVenue.venueLineArray
            }
        }

        val writer = FileWriter(file)
        writer.write(gson.toJson(concertsList))
        writer.close()
    }

    private fun getCurrentLocation(): Location {
        val location = Location("provider")
        location.latitude = position.latitude
        location.longitude = position.longitude
        return location
    }

    private fun saveLineArrayToFile(lineArray: LineArray) {
        val lineArraysFile = File(requireContext().filesDir, "linearrays.json")
        val gson = Gson()

        val lineArrayList: MutableList<LineArray> = if (lineArraysFile.exists()) {
            val reader = FileReader(lineArraysFile)
            val type = object : TypeToken<MutableList<LineArray>>() {}.type
            val existingList: MutableList<LineArray> = gson.fromJson(reader, type)
            reader.close()
            existingList
        } else {
            mutableListOf()
        }

        lineArrayList.add(lineArray)

        val writer = FileWriter(lineArraysFile)
        writer.write(gson.toJson(lineArrayList))
        writer.close()
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

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor == rotationSensor) {
            currentRotation = event.values[0] * 360
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}