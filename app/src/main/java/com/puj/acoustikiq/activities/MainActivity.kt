package com.puj.acoustikiq.activities

import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.puj.acoustikiq.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
        private lateinit var windowBinding: ActivityMainBinding

    companion object {
        const val REQUEST_CODE_MIC_PERMISSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        windowBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(windowBinding.root)


        //CODIGO PARA PERMISOS

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_CODE_MIC_PERMISSION)
        }

        //CODIGO PARA BOTONES MENU

        windowBinding.button1.setOnClickListener{
            println("BUTTON 1")
        }
        windowBinding.button2.setOnClickListener{
            val intentButton2 = Intent(this, SpectrumAnalysisActivity::class.java)
            startActivity(intentButton2)
            println("BUTTON 2")
        }
        windowBinding.button3.setOnClickListener{
            println("BUTTON 3")
        }
        windowBinding.button4.setOnClickListener{
            println("BUTTON 4")
        }
        windowBinding.button5.setOnClickListener{
            println("BUTTON 5")
        }
        windowBinding.button6.setOnClickListener{
            println("BUTTON 6")
        }
        windowBinding.button7.setOnClickListener{
            println("BUTTON 7")
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