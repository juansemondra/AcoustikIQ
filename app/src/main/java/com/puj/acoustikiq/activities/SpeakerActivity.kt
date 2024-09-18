package com.puj.acoustikiq.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.puj.acoustikiq.databinding.ActivitySpeakerBinding

class SpeakerActivity : AppCompatActivity() {

    private lateinit var speakerBinding: ActivitySpeakerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speakerBinding = ActivitySpeakerBinding.inflate(layoutInflater)
        setContentView(speakerBinding.root)

        speakerBinding.addSpeakerButton.setOnClickListener() {
            val intentSpeaker = Intent(this, MapActivity::class.java)
            startActivity(intentSpeaker)
        }

        speakerBinding.backButton.setOnClickListener() {
            val intentBack = Intent(this, MapActivity::class.java)
            startActivity(intentBack)
        }
    }
}
