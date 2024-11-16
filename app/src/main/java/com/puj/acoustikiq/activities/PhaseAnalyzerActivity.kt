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
import com.puj.acoustikiq.databinding.ActivityPhaseAnalyzerBinding
import com.puj.acoustikiq.fragments.PhaseAnalyzerFragment

class PhaseAnalyzerActivity : AppCompatActivity() {

    private lateinit var phaseAnalyzerBinding: ActivityPhaseAnalyzerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        phaseAnalyzerBinding = ActivityPhaseAnalyzerBinding.inflate(layoutInflater)
        setContentView(phaseAnalyzerBinding.root)

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_CODE_MIC_PERMISSION)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.phase_analyzer_fragment_container, PhaseAnalyzerFragment())
                .commit()
        }

        phaseAnalyzerBinding.backButton.setOnClickListener(){
            val backIntent = Intent(this, MenuActivity::class.java)
            startActivity(backIntent)
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