package com.puj.acoustikiq.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.puj.acoustikiq.databinding.ActivityStageBinding

class StageActivity : AppCompatActivity() {

    private lateinit var stageBinding: ActivityStageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stageBinding = ActivityStageBinding.inflate(layoutInflater)
        setContentView(stageBinding.root)

        stageBinding.viewMapButton.setOnClickListener(){
            val intentMap = Intent(this, MapActivity::class.java)
            startActivity(intentMap)
        }
        stageBinding.backButton.setOnClickListener(){
            val intentBack = Intent(this, EventDetailActivity::class.java)
            startActivity(intentBack)
        }
    }

}