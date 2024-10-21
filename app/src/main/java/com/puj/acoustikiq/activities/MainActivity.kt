package com.puj.acoustikiq.activities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.puj.acoustikiq.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    private lateinit var windowBinding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    companion object {
        const val REQUEST_CODE_MIC_PERMISSION = 1
        const val CONCERTS_JSON = "concerts.json"
        const val VENUES_JSON = "venues.json"
        const val SPEAKERS_JSON = "speakers.json"
        const val LINEARRAYS_JSON = "linearrays.json"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        windowBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(windowBinding.root)

        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        checkAndCopyJSONFiles()

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                REQUEST_CODE_MIC_PERMISSION
            )
        }

        windowBinding.button1.setOnClickListener {
            if (auth.currentUser != null) {
                val intentButton1 = Intent(this, ConcertActivity::class.java)
                startActivity(intentButton1)
                println("BUTTON 1")
            } else {
                Toast.makeText(this, "Tienes que hacer login primero", Toast.LENGTH_SHORT).show()
            }
        }

        windowBinding.button2.setOnClickListener {
            val intentButton2 = Intent(this, SpectrumAnalysisActivity::class.java)
            startActivity(intentButton2)
            println("BUTTON 2")
        }
        windowBinding.button3.setOnClickListener {
            val intentButton3 = Intent(this, LevelMeterActivity::class.java)
            startActivity(intentButton3)
            println("BUTTON 3")
        }
        windowBinding.button4.setOnClickListener {
            val intentButton4 = Intent(this, PhaseAnalyzerActivity::class.java)
            startActivity(intentButton4)
            println("BUTTON 4")
        }
        windowBinding.button8.setOnClickListener {
            val intentButton8 = Intent(this, MagnitudeActivity::class.java)
            startActivity(intentButton8)
            println("BUTTON 8")
        }
        windowBinding.button5.setOnClickListener {
            val intentButton5 = Intent(this, GalleryActivity::class.java)
            startActivity(intentButton5)
            println("BUTTON 5")
        }
        windowBinding.button6.setOnClickListener {
            println("BUTTON 6")
        }
        windowBinding.button7.setOnClickListener {
            println("BUTTON 7")
            finishAffinity()
        }
        windowBinding.button9.setOnClickListener {
            if (auth.currentUser != null) {
                auth.signOut()
                println("BUTTON 9")
                val intentButton9 = Intent(this, LoginActivity::class.java)
                startActivity(intentButton9)
            } else {
                println("BUTTON 9")
                val intentButton9 = Intent(this, LoginActivity::class.java)
                startActivity(intentButton9)
            }
        }
        windowBinding.button10.setOnClickListener {
            if (auth.currentUser != null) {
                println("BUTTON 10")
                val intentButton10 = Intent(this, ProfileActivity::class.java)
                startActivity(intentButton10)
            } else {
                Toast.makeText(this, "Tienes que hacer login primero", Toast.LENGTH_SHORT).show()
            }
        }

        windowBinding.button11.setOnClickListener {
            if (auth.currentUser != null) {
                println("BUTTON 11")
                val intentButton11 = Intent(this, RouteActivity::class.java)
                startActivity(intentButton11)
            } else {
                Toast.makeText(this, "Tienes que hacer login primero", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndCopyJSONFiles() {
        val filesDir = this.filesDir

        val concertsFile = File(filesDir, CONCERTS_JSON)
        if (!concertsFile.exists()) {
            copyAssetToInternalStorage(CONCERTS_JSON)
        }

        val venuesFile = File(filesDir, VENUES_JSON)
        if (!venuesFile.exists()) {
            copyAssetToInternalStorage(VENUES_JSON)
        }

        val speakersFile = File(filesDir, SPEAKERS_JSON)
        if (!speakersFile.exists()) {
            copyAssetToInternalStorage(SPEAKERS_JSON)
        }

        val lineArraysFile = File(filesDir, LINEARRAYS_JSON)
        if (!lineArraysFile.exists()) {
            copyAssetToInternalStorage(LINEARRAYS_JSON)
        }
    }

    private fun copyAssetToInternalStorage(fileName: String) {
        val assetManager = assets
        try {
            val inputStream: InputStream = assetManager.open(fileName)
            val outFile = File(filesDir, fileName)
            val outputStream = FileOutputStream(outFile)

            val buffer = ByteArray(1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }

            inputStream.close()
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error copiando $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_MIC_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        applicationContext,
                        "Gracias por confiar en nosotros!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Te recomendamos aceptar los permisos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}