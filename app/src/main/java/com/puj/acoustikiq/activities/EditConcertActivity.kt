package com.puj.acoustikiq.activities

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.puj.acoustikiq.databinding.ActivityEditConcertBinding
import com.puj.acoustikiq.model.Concert
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.puj.acoustikiq.adapters.DateTypeAdapter
import com.puj.acoustikiq.adapters.LocationAdapter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class EditConcertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditConcertBinding
    private lateinit var concert: Concert
    private lateinit var originalName: String
    private var concertsList: MutableList<Concert> = mutableListOf()
    private val concertsFileName = "concerts.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditConcertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        concert = intent.getParcelableExtra("concert")
            ?: throw NullPointerException("Concert object is missing in intent")

        originalName = concert.name

        loadConcertsFromJson()

        binding.concertNameEditText.setText(concert.name)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.concertDateEditText.setText(dateFormat.format(concert.date))

        binding.saveConcertButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadConcertsFromJson() {
        val concertsFile = File(filesDir, concertsFileName)
        if (!concertsFile.exists()) {
            Toast.makeText(this, "El archivo de conciertos no existe", Toast.LENGTH_SHORT).show()
            return
        }

        val gson = GsonBuilder()
            .registerTypeAdapter(Location::class.java, LocationAdapter())
            .registerTypeAdapter(Date::class.java, DateTypeAdapter())
            .create()

        val concertType = object : TypeToken<MutableList<Concert>>() {}.type
        val reader = FileReader(concertsFile)

        concertsList = gson.fromJson(reader, concertType)
        reader.close()
    }

    private fun saveChanges() {
        val newName = binding.concertNameEditText.text.toString()
        if (newName.isEmpty()) {
            Toast.makeText(this, "El nombre del concierto no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val newDate = try {
            dateFormat.parse(binding.concertDateEditText.text.toString()) ?: concert.date
        } catch (e: Exception) {
            Toast.makeText(this, "Formato de fecha inválido", Toast.LENGTH_SHORT).show()
            return
        }

        concert.name = newName
        concert.date = newDate

        val concertIndex = concertsList.indexOfFirst { it.name == originalName }
        if (concertIndex != -1) {
            concertsList[concertIndex] = concert
        } else {
            Toast.makeText(this, "El concierto no fue encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        saveConcertsToJson()

        Toast.makeText(this, "Concierto guardado exitosamente", Toast.LENGTH_SHORT).show()

        val concertIntent = Intent(this, ConcertActivity::class.java)
        startActivity(concertIntent)
        finish()
    }

    private fun saveConcertsToJson() {
        val concertsFile = File(filesDir, concertsFileName)

        val gson = GsonBuilder()
            .registerTypeAdapter(Location::class.java, LocationAdapter())
            .registerTypeAdapter(Date::class.java, DateTypeAdapter())
            .setPrettyPrinting()
            .create()

        val jsonString = gson.toJson(concertsList)

        concertsFile.outputStream().use { outputStream ->
            outputStream.write(jsonString.toByteArray())
            outputStream.flush()
        }
    }
}