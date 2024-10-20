package com.puj.acoustikiq.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.puj.acoustikiq.databinding.ActivityEditVenueBinding
import com.puj.acoustikiq.model.Venue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class EditVenueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditVenueBinding
    private lateinit var venue: Venue
    private var venueList: MutableList<Venue> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditVenueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        venue = intent.getParcelableExtra("venue")
            ?: throw NullPointerException("Venue object is missing in intent")

        loadVenuesFromJson()

        binding.venueNameEditText.setText(venue.name)
        binding.venueTemperatureEditText.setText(venue.temperature.toString())

        binding.saveVenueButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadVenuesFromJson() {
        val venuesFile = File(filesDir, "venues.json")
        if (!venuesFile.exists()) return

        val gson = Gson()
        val venueType = object : TypeToken<MutableList<Venue>>() {}.type
        val reader = FileReader(venuesFile)
        venueList = gson.fromJson(reader, venueType)
        reader.close()
    }

    private fun saveChanges() {
        venue.name = binding.venueNameEditText.text.toString()
        venue.temperature = binding.venueTemperatureEditText.text.toString().toDoubleOrNull() ?: venue.temperature

        val venueIndex = venueList.indexOfFirst { it.name == venue.name }
        if (venueIndex != -1) {
            venueList[venueIndex] = venue
        } else {
            venueList.add(venue)
        }

        saveVenuesToJson()

        val venueIntent = Intent(this, VenueActivity::class.java)
        startActivity(venueIntent)
    }

    private fun saveVenuesToJson() {
        val venuesFile = File(getExternalFilesDir(null), "venues.json")
        val gson = Gson()
        val jsonString = gson.toJson(venueList)

        venuesFile.outputStream().use { outputStream ->
            outputStream.write(jsonString.toByteArray())
            outputStream.flush()
        }
    }
}