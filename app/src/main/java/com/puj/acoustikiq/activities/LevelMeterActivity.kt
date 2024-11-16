package com.puj.acoustikiq.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.puj.acoustikiq.R
import com.puj.acoustikiq.activities.MenuActivity.Companion.REQUEST_CODE_MIC_PERMISSION
import com.puj.acoustikiq.databinding.ActivityLevelMeterBinding
import com.puj.acoustikiq.fragments.LevelMeterFragment

class LevelMeterActivity : AppCompatActivity() {

    private lateinit var levelMeterBinding: ActivityLevelMeterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        levelMeterBinding = ActivityLevelMeterBinding.inflate(layoutInflater)
        setContentView(levelMeterBinding.root)

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_CODE_MIC_PERMISSION)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.level_meter_fragment_container, LevelMeterFragment())
                .commit()
        }

        levelMeterBinding.backButton.setOnClickListener(){
            val intentBack = Intent(this, MenuActivity::class.java)
            startActivity(intentBack)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_MIC_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(applicationContext, "Gracias por confiar en nosotros!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "Te recomendamos aceptar los permisos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}