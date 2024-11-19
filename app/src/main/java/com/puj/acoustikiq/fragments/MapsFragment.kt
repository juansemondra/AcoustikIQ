package com.puj.acoustikiq.fragments

import android.annotation.SuppressLint
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
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.puj.acoustikiq.R
import com.puj.acoustikiq.adapters.SpeakerAdapter
import com.puj.acoustikiq.databinding.DialogAddLineArrayBinding
import com.puj.acoustikiq.databinding.DialogEditLineArrayBinding
import com.puj.acoustikiq.databinding.FragmentMapsBinding
import com.puj.acoustikiq.model.LineArray
import com.puj.acoustikiq.model.Speaker
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.model.Position
import com.puj.acoustikiq.model.Venue
import java.io.InputStreamReader

class MapsFragment : Fragment() {

    private lateinit var binding: FragmentMapsBinding
    private lateinit var gMap: GoogleMap
    private val database = FirebaseDatabase.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val markers = mutableListOf<Marker>()
    private var speakers: List<Speaker> = emptyList()

    private lateinit var concert: Concert
    private lateinit var venue: Venue
    private var positionMarker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapsBinding.inflate(inflater, container, false)

        concert = arguments?.getParcelable("concert")
            ?: throw IllegalStateException("Concert object missing in arguments")
        venue = arguments?.getParcelable("venue")
            ?: throw IllegalStateException("Venue object missing in arguments")

        loadSpeakersFromJson()

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            gMap = googleMap
            gMap.uiSettings.isZoomControlsEnabled = true
            loadLineArrays()
            setupMapListeners()
        }

        binding.addLineArrayButton.setOnClickListener { showAddLineArrayDialog() }

        return binding.root
    }

    private fun loadSpeakersFromJson() {
        try {
            val inputStream = requireContext().assets.open("speakers.json")
            val reader = InputStreamReader(inputStream)
            val type = object : TypeToken<List<Speaker>>() {}.type
            speakers = Gson().fromJson(reader, type)
            reader.close()
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading speakers: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateUserPosition(latLng: LatLng) {
        if (::gMap.isInitialized) {
            if (positionMarker == null) {
                positionMarker = gMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("Tu ubicación")
                        .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.ic_position_marker))
                )
            } else {
                positionMarker?.position = latLng
            }
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    private fun saveLineArray(lineArray: LineArray) {
        val ref = database.getReference(
            "concerts/users/${currentUser?.uid}/${concert.id}/venues/${venue.id}/linearrays"
        )
        val lineArrayId = ref.push().key ?: return
        lineArray.id = lineArrayId

        ref.child(lineArrayId).setValue(lineArray)
            .addOnSuccessListener {
                Toast.makeText(context, "Line Array creado exitosamente", Toast.LENGTH_SHORT).show()
                addMarkerForLineArray(lineArray)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al crear Line Array", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadLineArrays() {
        val ref = database.getReference(
            "concerts/users/${currentUser?.uid}/${concert.id}/venues/${venue.id}/linearrays"
        )
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                markers.forEach { it.remove() }
                markers.clear()

                snapshot.children.forEach { child ->
                    val map = child.value as? Map<*, *>
                    if (map != null) {
                        val locationMap = map["location"] as? Map<*, *>
                        val lineArray = LineArray(
                            id = map["id"] as? String,
                            type = map["type"] as? String ?: "",
                            system = Gson().fromJson(Gson().toJson(map["system"]), Speaker::class.java),
                            quantity = (map["quantity"] as? Long)?.toInt() ?: 0,
                            location = Position(
                                latitude = locationMap?.get("latitude") as? Double ?: 0.0,
                                longitude = locationMap?.get("longitude") as? Double ?: 0.0
                            ),
                            delay = (map["delay"] as? Double)?.toFloat() ?: 0f,
                            calibratedFreq = map["calibratedFreq"] as? Boolean ?: false,
                            calibratedPhase = map["calibratedPhase"] as? Boolean ?: false
                        )
                        addMarkerForLineArray(lineArray)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error al cargar line arrays: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addMarkerForLineArray(lineArray: LineArray) {
        val position = LatLng(lineArray.location.latitude, lineArray.location.longitude)
        val marker = gMap.addMarker(
            MarkerOptions()
                .position(position)
                .title(lineArray.type)
                .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.ic_line_array))
        )
        marker?.tag = lineArray
        markers.add(marker!!)
    }
    @SuppressLint("PotentialBehaviorOverride")
    private fun setupMapListeners() {
        gMap.setOnMarkerClickListener { marker ->
            val lineArray = marker.tag as? LineArray
            if (lineArray != null) showEditLineArrayDialog(lineArray, marker)
            true
        }
    }

    private fun showAddLineArrayDialog() {
        val dialogBinding = DialogAddLineArrayBinding.inflate(LayoutInflater.from(context))
        val speakerAdapter = SpeakerAdapter(requireContext(), android.R.layout.simple_spinner_item, speakers)
        dialogBinding.speakerDropdown.adapter = speakerAdapter

        AlertDialog.Builder(requireContext())
            .setTitle("Agregar Line Array")
            .setView(dialogBinding.root)
            .setPositiveButton("Crear") { _, _ ->
                val speaker = dialogBinding.speakerDropdown.selectedItem as? Speaker
                val quantity = dialogBinding.quantityInput.text.toString().toIntOrNull()

                if (speaker != null && quantity != null) {
                    val lineArray = LineArray(
                        id = null,
                        type = speaker.model,
                        system = speaker,
                        quantity = quantity,
                        location = Position(
                            latitude = gMap.cameraPosition.target.latitude,
                            longitude = gMap.cameraPosition.target.longitude
                        ),
                        delay = 0f,
                        calibratedFreq = false,
                        calibratedPhase = false
                    )
                    saveLineArray(lineArray)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditLineArrayDialog(lineArray: LineArray, marker: Marker) {
        val dialogBinding = DialogEditLineArrayBinding.inflate(LayoutInflater.from(context))

        dialogBinding.quantityInput.setText(lineArray.quantity.toString())
        dialogBinding.delayInput.setText(lineArray.delay.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Editar Line Array")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val newQuantity = dialogBinding.quantityInput.text.toString().toIntOrNull() ?: lineArray.quantity
                val newDelay = dialogBinding.delayInput.text.toString().toFloatOrNull() ?: lineArray.delay
                lineArray.quantity = newQuantity
                lineArray.delay = newDelay

                val ref = database.getReference(
                    "concerts/users/${currentUser?.uid}/${concert.id}/venues/${venue.id}/linearrays/${lineArray.id}"
                )

                ref.setValue(lineArray)
                    .addOnSuccessListener {
                        marker.title = lineArray.type
                        Toast.makeText(context, "Line Array actualizado exitosamente", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error al actualizar Line Array", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId) ?: return null
        vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}