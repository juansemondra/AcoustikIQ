package com.puj.acoustikiq.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.puj.acoustikiq.databinding.ActivityEditVenueBinding
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.model.Position
import com.puj.acoustikiq.model.Venue
import java.io.Serializable

class EditVenueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditVenueBinding
    private lateinit var venue: Venue
    private lateinit var concert: Concert
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditVenueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        venue = intent.getParcelableExtra("venue")
            ?: throw NullPointerException("Venue object is missing in intent")

        concert = intent.getParcelableExtra("concert")
            ?: throw NullPointerException("Concert object is missing in intent")

        database = FirebaseDatabase.getInstance().getReference("concerts/users/${FirebaseAuth.getInstance().currentUser?.uid}")

        populateFields()

        binding.saveVenueButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun populateFields() {
        binding.venueNameEditText.setText(venue.name)
        binding.venueTemperatureEditText.setText(venue.temperature.toString())
        binding.venueLatitudeEditText.setText(venue.position.latitude.toString())
        binding.venueLongitudeEditText.setText(venue.position.longitude.toString())
    }

    private fun saveChanges() {
        val newName = binding.venueNameEditText.text.toString()
        val newTemperature = binding.venueTemperatureEditText.text.toString().toDoubleOrNull()
        val newLatitude = binding.venueLatitudeEditText.text.toString().toDoubleOrNull()
        val newLongitude = binding.venueLongitudeEditText.text.toString().toDoubleOrNull()

        if (newName.isEmpty() || newTemperature == null || newLatitude == null || newLongitude == null) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        venue.name = newName
        venue.temperature = newTemperature
        venue.position = Position(latitude = newLatitude, longitude = newLongitude)

        val venuePath = "${concert.id}/venues/${venue.id}"

        database.child(venuePath)
            .setValue(venue)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Venue actualizado exitosamente", Toast.LENGTH_SHORT).show()
                    navigateToVenueActivity()
                } else {
                    Toast.makeText(this, "Error al actualizar el venue", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToVenueActivity() {
        val venueIntent = Intent(this, VenueActivity::class.java)
        venueIntent.putExtra("concert", concert as Serializable)
        startActivity(venueIntent)
        finish()
    }
}