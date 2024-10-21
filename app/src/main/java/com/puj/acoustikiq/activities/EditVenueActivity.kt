package com.puj.acoustikiq.activities

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.puj.acoustikiq.adapters.DateTypeAdapter
import com.puj.acoustikiq.adapters.LocationAdapter
import com.puj.acoustikiq.databinding.ActivityEditVenueBinding
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.model.Venue
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.Date

class EditVenueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditVenueBinding
    private lateinit var venue: Venue
    private lateinit var concert: Concert
    private var venueList: MutableList<Venue> = mutableListOf()
    private var concertsList: MutableList<Concert> = mutableListOf()
    private val venuesFileName = "venues.json"
    private val concertsFileName = "concerts.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditVenueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        venue = intent.getParcelableExtra("venue")
            ?: throw NullPointerException("Venue object is missing in intent")

        concert = intent.getParcelableExtra("concert")
            ?: throw NullPointerException("Concert object is missing in intent")

        loadVenuesFromJson()
        loadConcertsFromJson()

        binding.venueNameEditText.setText(venue.name)
        binding.venueTemperatureEditText.setText(venue.temperature.toString())

        binding.saveVenueButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadVenuesFromJson() {
        val venuesFile = File(filesDir, venuesFileName)
        if (!venuesFile.exists()) return

        val gson = GsonBuilder()
            .registerTypeAdapter(Location::class.java, LocationAdapter())
            .create()

        val venueType = object : TypeToken<MutableList<Venue>>() {}.type
        val reader = FileReader(venuesFile)
        venueList = gson.fromJson(reader, venueType)
        reader.close()
    }

    private fun loadConcertsFromJson() {
        val concertsFile = File(filesDir, concertsFileName)
        if (!concertsFile.exists()) return

        val gson = GsonBuilder()
            .registerTypeAdapter(Date::class.java, DateTypeAdapter())
            .registerTypeAdapter(Location::class.java, LocationAdapter())
            .create()

        val concertType = object : TypeToken<MutableList<Concert>>() {}.type
        val reader = FileReader(concertsFile)
        concertsList = gson.fromJson(reader, concertType)
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

        val concertIndex = concertsList.indexOfFirst { it.name == concert.name }
        if (concertIndex != -1) {
            val concertToUpdate = concertsList[concertIndex]
            val venueInConcertIndex = concertToUpdate.venues.indexOfFirst { it.name == venue.name }

            if (venueInConcertIndex != -1) {
                concertToUpdate.venues[venueInConcertIndex] = venue
            } else {
                concertToUpdate.venues.add(venue)
            }

            concertsList[concertIndex] = concertToUpdate
        } else {
            Toast.makeText(this, "Concierto no encontrado", Toast.LENGTH_SHORT).show()
        }

        saveVenuesToJson()
        saveConcertsToJson()

        Toast.makeText(this, "Venue actualizado exitosamente", Toast.LENGTH_SHORT).show()

        val venueIntent = Intent(this, VenueActivity::class.java)
        venueIntent.putExtra("concert", concert)
        startActivity(venueIntent)
        finish()
    }

    private fun saveVenuesToJson() {
        val venuesFile = File(filesDir, venuesFileName)

        val gson = GsonBuilder()
            .registerTypeAdapter(Location::class.java, LocationAdapter())
            .setPrettyPrinting()
            .create()

        val jsonString = gson.toJson(venueList)

        venuesFile.outputStream().use { outputStream ->
            outputStream.write(jsonString.toByteArray())
            outputStream.flush()
        }
    }

    private fun saveConcertsToJson() {
        val concertsFile = File(filesDir, concertsFileName)

        val gson = GsonBuilder()
            .registerTypeAdapter(Date::class.java, DateTypeAdapter())
            .registerTypeAdapter(Location::class.java, LocationAdapter())
            .setPrettyPrinting()
            .create()

        val jsonString = gson.toJson(concertsList)

        concertsFile.outputStream().use { outputStream ->
            outputStream.write(jsonString.toByteArray())
            outputStream.flush()
        }
    }
}