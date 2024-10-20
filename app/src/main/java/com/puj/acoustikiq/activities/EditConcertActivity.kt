package com.puj.acoustikiq.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.puj.acoustikiq.databinding.ActivityEditConcertBinding
import com.puj.acoustikiq.model.Concert
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class EditConcertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditConcertBinding
    private lateinit var concert: Concert
    private var concertsList: MutableList<Concert> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditConcertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        concert = intent.getParcelableExtra("concert")
            ?: throw NullPointerException("Concert object is missing in intent")

        loadConcertsFromJson()

        binding.concertNameEditText.setText(concert.name)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.concertDateEditText.setText(dateFormat.format(concert.date))

        binding.saveConcertButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadConcertsFromJson() {
        val concertsFile = File(filesDir, "concerts.json")
        if (!concertsFile.exists()) return

        val gson = Gson()
        val concertType = object : TypeToken<MutableList<Concert>>() {}.type
        val reader = FileReader(concertsFile)
        concertsList = gson.fromJson(reader, concertType)
        reader.close()
    }

    private fun saveChanges() {
        concert.name = binding.concertNameEditText.text.toString()

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val newDate = dateFormat.parse(binding.concertDateEditText.text.toString()) ?: concert.date
        concert.date = newDate

        val concertIndex = concertsList.indexOfFirst { it.name == concert.name }
        if (concertIndex != -1) {
            concertsList[concertIndex] = concert
        } else {
            concertsList.add(concert)
        }

        saveConcertsToJson()

        val concertIntent = Intent(this, ConcertActivity::class.java)
        startActivity(concertIntent)
    }

    private fun saveConcertsToJson() {
        println("SAVE CONCERT TO JSON FUNC")
        val concertsFile = File(getExternalFilesDir(null), "concerts.json")
        val gson = Gson()
        val jsonString = gson.toJson(concertsList)

        concertsFile.outputStream().use { outputStream ->
            outputStream.write(jsonString.toByteArray())
            outputStream.flush()
        }
    }
}