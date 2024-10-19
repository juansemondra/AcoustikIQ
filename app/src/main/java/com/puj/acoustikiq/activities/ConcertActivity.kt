package com.puj.acoustikiq.activities

import ConcertAdapter
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.puj.acoustikiq.adapters.DateTypeAdapter
import com.puj.acoustikiq.databinding.ActivityConcertBinding
import com.puj.acoustikiq.model.Concert
import java.io.InputStreamReader
import java.io.File
import java.io.FileReader
import java.util.Date

class ConcertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConcertBinding
    private lateinit var concertAdapter: ConcertAdapter
    private var concertList: List<Concert> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityConcertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        concertList = loadConcertsFromJson()

        concertAdapter = ConcertAdapter(concertList, ::onConcertClick)
        binding.concertRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.concertRecyclerView.adapter = concertAdapter
    }

    private fun loadConcertsFromJson(): List<Concert> {
        val assetManager = assets
        val inputStream = assetManager.open("concerts.json")
        val reader = InputStreamReader(inputStream)

        val gson = GsonBuilder()
            .registerTypeAdapter(Date::class.java, DateTypeAdapter())
            .create()

        val concertType = object : TypeToken<List<Concert>>() {}.type
        return gson.fromJson(reader, concertType)
    }

    private fun onConcertClick(concert: Concert) {
        AlertDialog.Builder(this)
            .setTitle("Opciones de Concierto")
            .setMessage("Elija una opción para el concierto ${concert.name}")
            .setPositiveButton("Editar") { _, _ ->
                val editIntent = Intent(this, EditConcertActivity::class.java)
                editIntent.putExtra("concert", concert)
                startActivity(editIntent)
            }
            .setNegativeButton("Ver Venues") { _, _ ->
                val venueIntent = Intent(this, VenueActivity::class.java)
                venueIntent.putExtra("concert", concert)
                startActivity(venueIntent)
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }
}