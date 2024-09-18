package com.puj.acoustikiq.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.puj.acoustikiq.databinding.ActivityEventBinding

class EventActivity : AppCompatActivity() {

    private lateinit var eventBinding: ActivityEventBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        eventBinding = ActivityEventBinding.inflate(layoutInflater)
        setContentView(eventBinding.root)

        eventBinding.createEventButton.setOnClickListener(){
            val intentEvent = Intent(this, EventDetailActivity::class.java)
            startActivity(intentEvent)
        }

        eventBinding.selectEventButton.setOnClickListener(){
            val intentEvent = Intent(this, EventDetailActivity::class.java)
            startActivity(intentEvent)
        }

        eventBinding.backButton.setOnClickListener(){
            val intentBack = Intent(this, MainActivity::class.java)
            startActivity(intentBack)
        }
    }

}