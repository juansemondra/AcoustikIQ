package com.puj.acoustikiq.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.puj.acoustikiq.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity() {

    private lateinit var mapBinding: ActivityMapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mapBinding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(mapBinding.root)

        mapBinding.addSpeakerButton.setOnClickListener(){
            val intentSpeaker = Intent(this, SpeakerActivity::class.java)
            startActivity(intentSpeaker)
        }

        mapBinding.backButton.setOnClickListener(){
            val intentBack = Intent(this, StageActivity::class.java)
            startActivity(intentBack)
        }
    }

}