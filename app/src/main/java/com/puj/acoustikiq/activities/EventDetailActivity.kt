package com.puj.acoustikiq.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.puj.acoustikiq.databinding.ActivityEventDetailBinding


class EventDetailActivity : AppCompatActivity() {

    private lateinit var eventDetailBinding: ActivityEventDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        eventDetailBinding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(eventDetailBinding.root)

        eventDetailBinding.editEventButton.setOnClickListener(){
            val intentStage = Intent(this, StageActivity::class.java)
            startActivity(intentStage)
        }

        eventDetailBinding.backButton.setOnClickListener(){
            val intentBack = Intent(this, EventActivity::class.java)
            startActivity(intentBack)
        }
    }

}