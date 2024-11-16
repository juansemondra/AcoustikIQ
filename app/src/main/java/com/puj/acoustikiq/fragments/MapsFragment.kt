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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.puj.acoustikiq.R
import com.puj.acoustikiq.databinding.DialogAddLineArrayBinding
import com.puj.acoustikiq.databinding.DialogEditLineArrayBinding
import com.puj.acoustikiq.databinding.FragmentMapsBinding
import com.puj.acoustikiq.model.LineArray
import com.puj.acoustikiq.model.Speaker
import com.puj.acoustikiq.adapters.SpeakerAdapter
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.model.Venue

class MapsFragment : Fragment() {

    private lateinit var binding: FragmentMapsBinding
    private lateinit var gMap: GoogleMap
    private lateinit var currentLineArrays: MutableList<LineArray>
    private var positionMarker: Marker? = null
    private var position = LatLng(-34.0, 151.0)
    private val database = FirebaseDatabase.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val markers: MutableList<Marker> = mutableListOf()

    private lateinit var venueId: String
    private lateinit var concertId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapsBinding.inflate(inflater, container, false)

        venueId = arguments?.getParcelable<Venue>("venue")?.id
            ?: throw IllegalStateException("Venue ID not found in arguments")
        concertId = arguments?.getParcelable<Concert>("concert")?.id
            ?: throw IllegalStateException("Concert ID not found in arguments")

        currentLineArrays = mutableListOf()

        val callback = OnMapReadyCallback { googleMap ->
            gMap = googleMap
            gMap.uiSettings.isZoomControlsEnabled = true
            loadLineArraysFromFirebase()

            gMap.setOnMarkerClickListener { marker ->
                val lineArray = marker.tag as? LineArray
                if (lineArray != null) {
                    showEditLineArrayDialog(lineArray, marker)
                }
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

    fun moveFunction(location: Location) {
        val position = LatLng(location.latitude, location.longitude)
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
    }

    fun updateSpeakers(lineArrayList: List<LineArray>) {
        markers.forEach { it.remove() }
        markers.clear()

        lineArrayList.forEach { lineArray ->
            val location = LatLng(lineArray.location.latitude, lineArray.location.longitude)
            val marker = gMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(lineArray.system.model)
                    .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.military_tech_24px))
            )
            marker?.tag = lineArray
            markers.add(marker!!)
        }
    }

    private fun loadLineArraysFromFirebase() {
        val lineArraysRef = database.getReference(
            "concerts/users/${currentUser?.uid}/$concertId/venues/$venueId/linearrays"
        )

        lineArraysRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentLineArrays.clear()
                gMap.clear()

                for (lineArraySnapshot in snapshot.children) {
                    val lineArray = lineArraySnapshot.getValue(LineArray::class.java)
                    lineArray?.let {
                        currentLineArrays.add(it)
                        val location = LatLng(it.location.latitude, it.location.longitude)
                        val marker = gMap.addMarker(
                            MarkerOptions()
                                .position(location)
                                .title(it.system.model)
                                .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.military_tech_24px))
                        )
                        marker?.tag = it
                    }
                }

                if (currentLineArrays.isNotEmpty()) {
                    gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error al cargar LineArrays: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveLineArrayToFirebase(lineArray: LineArray) {
        val lineArrayId = database.reference.push().key ?: return
        val lineArraysRef = database.getReference(
            "concerts/users/${currentUser?.uid}/$concertId/venues/$venueId/linearrays/$lineArrayId"
        )

        lineArray.id = lineArrayId
        lineArraysRef.setValue(lineArray)
            .addOnSuccessListener {
                Toast.makeText(context, "LineArray agregado exitosamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al agregar LineArray", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateLineArrayInFirebase(lineArray: LineArray, marker: Marker) {
        val lineArrayId = lineArray.id ?: return
        val lineArraysRef = database.getReference(
            "concerts/users/${currentUser?.uid}/$concertId/venues/$venueId/linearrays/$lineArrayId"
        )

        lineArraysRef.setValue(lineArray)
            .addOnSuccessListener {
                marker.title = lineArray.system.model
                Toast.makeText(context, "LineArray actualizado exitosamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al actualizar LineArray", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddLineArrayDialog() {
        val dialogBinding = DialogAddLineArrayBinding.inflate(LayoutInflater.from(context))
        val speakerAdapter = SpeakerAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf()) // Lista de speakers
        dialogBinding.speakerDropdown.adapter = speakerAdapter

        AlertDialog.Builder(requireContext())
            .setTitle("Agregar Line Array")
            .setView(dialogBinding.root)
            .setPositiveButton("Crear") { _, _ ->
                val selectedSpeaker = dialogBinding.speakerDropdown.selectedItem as Speaker
                val quantity = dialogBinding.quantityInput.text.toString().toIntOrNull() ?: return@setPositiveButton

                val newLineArray = LineArray(
                    id = null,
                    type = selectedSpeaker.model,
                    system = selectedSpeaker,
                    quantity = quantity,
                    location = position.toLocation(),
                    delay = 0f,
                    calibratedFreq = false,
                    calibratedPhase = false
                )

                saveLineArrayToFirebase(newLineArray)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditLineArrayDialog(lineArray: LineArray, marker: Marker) {
        val dialogBinding = DialogEditLineArrayBinding.inflate(LayoutInflater.from(context))

        dialogBinding.quantityInput.setText(lineArray.quantity.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Editar Line Array")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val newQuantity = dialogBinding.quantityInput.text.toString().toIntOrNull() ?: lineArray.quantity
                lineArray.quantity = newQuantity
                updateLineArrayInFirebase(lineArray, marker)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun LatLng.toLocation(): Location {
        val location = Location("LatLng")
        location.latitude = this.latitude
        location.longitude = this.longitude
        return location
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