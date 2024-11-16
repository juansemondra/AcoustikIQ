package com.puj.acoustikiq.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.puj.acoustikiq.databinding.ActivityEditConcertBinding
import com.puj.acoustikiq.model.Concert
import com.puj.acoustikiq.model.Position
import java.text.SimpleDateFormat
import java.util.*

class EditConcertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditConcertBinding
    private lateinit var concert: Concert

    private val auth = FirebaseAuth.getInstance()
    private val database: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("concerts/users/${auth.currentUser?.uid}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditConcertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        concert = intent.getParcelableExtra("concert")
            ?: throw NullPointerException("Concert object is missing in intent")

        populateFields()

        binding.saveConcertButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun populateFields() {
        binding.concertNameEditText.setText(concert.name)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.concertDateEditText.setText(dateFormat.format(concert.date))
    }

    private fun saveChanges() {
        val newName = binding.concertNameEditText.text.toString()
        if (newName.isEmpty()) {
            Toast.makeText(this, "El nombre del concierto no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val newDate = try {
            dateFormat.parse(binding.concertDateEditText.text.toString())
        } catch (e: Exception) {
            Toast.makeText(this, "Formato de fecha inválido", Toast.LENGTH_SHORT).show()
            return
        }

        val newPosition = concert.location

        if (newDate != null) {
            concert.date = newDate.time
        }
        concert.name = newName
        concert.location = newPosition

        updateConcertInDatabase()
    }

    private fun updateConcertInDatabase() {
        val userConcertRef = database.child(concert.id)

        userConcertRef.setValue(concert)
            .addOnSuccessListener {
                Toast.makeText(this, "Concierto actualizado exitosamente", Toast.LENGTH_SHORT).show()
                navigateToConcertActivity()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar el concierto: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun navigateToConcertActivity() {
        val concertIntent = Intent(this, ConcertActivity::class.java)
        startActivity(concertIntent)
        finish()
    }
}