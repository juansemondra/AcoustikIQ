package com.puj.acoustikiq.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.puj.acoustikiq.R
import com.puj.acoustikiq.databinding.FragmentMapsBinding
import com.puj.acoustikiq.databinding.DialogAddLineArrayBinding
import com.puj.acoustikiq.model.LineArray
import com.puj.acoustikiq.model.Speaker
import com.puj.acoustikiq.model.Venue
import com.puj.acoustikiq.adapters.SpeakerAdapter
import com.puj.acoustikiq.databinding.DialogEditLineArrayBinding
import com.puj.acoustikiq.model.Concert
import java.io.File
import java.io.InputStreamReader

class MapsFragment : Fragment() {

    private lateinit var binding: FragmentMapsBinding
    private lateinit var gMap: GoogleMap
    private lateinit var venue: Venue
    private lateinit var concert: Concert
    private var speakersList: MutableList<Speaker> = mutableListOf()
    private var position: LatLng = LatLng(-34.0, 151.0)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapsBinding.inflate(inflater, container, false)

        venue = arguments?.getParcelable("venue")
            ?: throw IllegalStateException("Venue not found in arguments")
        concert = arguments?.getParcelable("concert")
            ?: throw IllegalStateException("Concert not found in arguments")

        speakersList = loadSpeakersFromJson()

        val callback = OnMapReadyCallback { googleMap ->
            gMap = googleMap
            gMap.uiSettings.isZoomControlsEnabled = false

            venue.venueLineArray.forEach { lineArray ->
                val location = LatLng(lineArray.location.latitude, lineArray.location.longitude)
                val marker = gMap.addMarker(
                    MarkerOptions().position(location).title(lineArray.system.model)
                        .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.military_tech_24px))
                )
                marker?.tag = lineArray
            }

            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))

            gMap.setOnMarkerClickListener { marker ->
                val lineArray = marker.tag as? LineArray
                lineArray?.let { showEditLineArrayDialog(it, marker) }
                true
            }
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also {
                childFragmentManager.beginTransaction().replace(R.id.map, it).commit()
            }

        mapFragment.getMapAsync(callback)

        binding.addLineArrayButton.setOnClickListener { showAddLineArrayDialog() }

        return binding.root
    }

    private fun showEditLineArrayDialog(lineArray: LineArray, marker: Marker) {
        val dialogBinding = DialogEditLineArrayBinding.inflate(LayoutInflater.from(context))

        val speakerAdapter = SpeakerAdapter(requireContext(), android.R.layout.simple_spinner_item, speakersList)
        dialogBinding.speakerDropdown.adapter = speakerAdapter

        val currentSpeakerIndex = speakersList.indexOfFirst { it.model == lineArray.system.model }
        dialogBinding.speakerDropdown.setSelection(currentSpeakerIndex)

        dialogBinding.quantityInput.setText(lineArray.quantity.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Editar Line Array")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val selectedSpeaker = dialogBinding.speakerDropdown.selectedItem as Speaker
                val newQuantity = dialogBinding.quantityInput.text.toString().toIntOrNull() ?: lineArray.quantity

                lineArray.system = selectedSpeaker
                lineArray.quantity = newQuantity
                marker.title = selectedSpeaker.model

                saveLineArrayToFiles(lineArray)

                Toast.makeText(requireContext(), "Line Array actualizado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
                    .position(LatLng(position.latitude, position.longitude))
                    .title(newLineArray.type)
                    .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.military_tech_24px))

                gMap.addMarker(markerOptions)
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(position.latitude, position.longitude), 15f))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveLineArrayToFiles(lineArray: LineArray) {
        try {
            saveLineArrayToFile(lineArray)
            updateVenueInFile(venue, "venues.json")
            updateConcertInFile(venue, "concerts.json")
            Toast.makeText(requireContext(), "Archivos guardados correctamente", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al guardar archivos: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveLineArrayToFile(lineArray: LineArray) {
        val lineArraysFile = File(requireContext().getExternalFilesDir(null), "linearrays.json")
        val gson = Gson()

        val lineArrayList: MutableList<LineArray> = if (lineArraysFile.exists()) {
            lineArraysFile.bufferedReader().use { reader ->
                val type = object : TypeToken<MutableList<LineArray>>() {}.type
                gson.fromJson(reader, type)
            }
        } else {
            mutableListOf()
        }

        lineArrayList.add(lineArray)

        lineArraysFile.bufferedWriter().use { writer ->
            writer.write(gson.toJson(lineArrayList))
        }
    }

    private fun updateVenueInFile(updatedVenue: Venue, fileName: String) {
        val gson = Gson()

        val file = File(requireContext().getExternalFilesDir(null), fileName)

        val venuesList: MutableList<Venue> = if (file.exists()) {
            val reader = file.bufferedReader()
            val type = object : TypeToken<MutableList<Venue>>() {}.type
            gson.fromJson(reader, type)
        } else {
            mutableListOf()
        }

        val venueIndex = venuesList.indexOfFirst { it.name == updatedVenue.name }
        if (venueIndex != -1) {
            venuesList[venueIndex] = updatedVenue
        } else {
            venuesList.add(updatedVenue)
        }

        file.bufferedWriter().use { writer ->
            writer.write(gson.toJson(venuesList))
        }
    }

    private fun updateConcertInFile(updatedVenue: Venue, fileName: String) {
        val gson = Gson()

        val file = File(requireContext().getExternalFilesDir(null), fileName)

        val concertsList: MutableList<Concert> = if (file.exists()) {
            val reader = file.bufferedReader()
            val type = object : TypeToken<MutableList<Concert>>() {}.type
            gson.fromJson(reader, type)
        } else {
            mutableListOf()
        }

        concertsList.forEach { concert ->
            concert.venues.find { it.name == updatedVenue.name }?.let { foundVenue ->
                foundVenue.venueLineArray = updatedVenue.venueLineArray
            }
        }

        file.bufferedWriter().use { writer ->
            writer.write(gson.toJson(concertsList))
        }
    }

    private fun loadSpeakersFromJson(): MutableList<Speaker> {
        val assetManager = requireContext().assets
        val inputStream = assetManager.open("speakers.json")
        val reader = InputStreamReader(inputStream)

        val gson = GsonBuilder().create()

        val speakerType = object : TypeToken<List<Speaker>>() {}.type

        return gson.fromJson(reader, speakerType)
    }

    private fun getCurrentLocation(): Location {
        val location = Location("provider")
        location.latitude = position.latitude
        location.longitude = position.longitude
        return location
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
}