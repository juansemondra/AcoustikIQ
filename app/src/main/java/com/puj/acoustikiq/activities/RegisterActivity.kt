package com.puj.acoustikiq.activities

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.puj.acoustikiq.R
import com.puj.acoustikiq.databinding.ActivityRegisterBinding
import com.puj.acoustikiq.model.UserProfile
import com.puj.acoustikiq.util.Alerts
import java.io.File

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val TAG = RegisterActivity::class.java.simpleName

    private val auth = Firebase.auth
    private val database = Firebase.database
    private val storage = Firebase.storage

    private val alerts = Alerts(this)

    private val PERM_CAMERA_CODE = 101
    private val REQUEST_IMAGE_CAPTURE = 10
    private val PERM_GALLERY_CODE = 202
    private val REQUEST_PICK = 3
    private var userPhotoPath: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkCameraPermission()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.signupButton.setOnClickListener { signUp() }
        binding.regPhotoBtn.setOnClickListener { takePhoto() }
        binding.regGalleryBtn.setOnClickListener { startGallery() }
        binding.backButton.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                alerts.indefiniteSnackbar(binding.root, "El permiso de cámara es necesario para usar esta actividad.")
            }
            else -> {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), PERM_CAMERA_CODE)
            }
        }
    }

    private fun signUp() {
        if (validateFields()) {
            disableFields()
            val email = binding.signupEmail.editText?.text.toString()
            val password = binding.signupPass.editText?.text.toString()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                        val storageRef = storage.reference.child("users/$uid/profile.jpg")

                        if (userPhotoPath != null) {
                            val localFile = File(userPhotoPath!!.path!!)
                            if (localFile.exists()) {
                                storageRef.putFile(userPhotoPath!!)
                                    .addOnSuccessListener {
                                        storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                            saveUserProfile(downloadUri.toString())
                                        }.addOnFailureListener { error ->
                                            enableFields()
                                            alerts.showErrorDialog(
                                                "Error al obtener la URL de descarga",
                                                error.localizedMessage ?: "Error desconocido"
                                            )
                                        }
                                    }
                                    .addOnFailureListener { error ->
                                        enableFields()
                                        alerts.showErrorDialog(
                                            "Error al subir la imagen",
                                            error.localizedMessage ?: "Error desconocido"
                                        )
                                    }
                            } else {
                                enableFields()
                                alerts.showErrorDialog("Error", "El archivo local no existe.")
                            }
                        } else {
                            enableFields()
                            alerts.showErrorDialog("Error", "No se seleccionó ninguna foto para subir.")
                        }
                    } else {
                        enableFields()
                        alerts.showErrorDialog(
                            "Error al crear el usuario",
                            task.exception?.localizedMessage ?: "Error desconocido"
                        )
                    }
                }
        }
    }

    private fun saveUserProfile(photoUrl: String) {
        val uid = auth.currentUser?.uid ?: return
        val userProfile = UserProfile(
            name = binding.signupName.editText?.text.toString(),
            phone = binding.signupPhone.editText?.text.toString(),
            photoUrl = photoUrl
        )

        database.reference.child("users/$uid").setValue(userProfile)
            .addOnCompleteListener {
                startActivity(Intent(this, MenuActivity::class.java))
                finish()
            }
            .addOnFailureListener { error ->
                enableFields()
                alerts.showErrorDialog(
                    "Error al guardar el perfil",
                    error.localizedMessage ?: "Error desconocido"
                )
            }
    }

    private fun takePhoto() {
        val imageFileName = "JPEG_${System.currentTimeMillis()}.jpg"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(storageDir, imageFileName)

        userPhotoPath = FileProvider.getUriForFile(
            this,
            "com.puj.acoustikiq.fileprovider",
            imageFile
        )

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, userPhotoPath)
        }

        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            alerts.showErrorDialog("Error al tomar la foto", e.localizedMessage ?: "Error desconocido")
        }
    }

    private fun startGallery() {
        val intentPick = Intent(Intent.ACTION_PICK)
        intentPick.type = "image/*"
        startActivityForResult(intentPick, REQUEST_PICK)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERM_CAMERA_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    alerts.shortSimpleSnackbar(binding.root, "Permiso de cámara denegado.")
                }
            }
            PERM_GALLERY_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) startGallery()
                else alerts.shortSimpleSnackbar(binding.root, "Permiso de galería denegado.")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                if (resultCode == RESULT_OK) {
                    displaySelectedImage(userPhotoPath)
                } else {
                    alerts.shortSimpleSnackbar(binding.root, "No se pudo tomar la foto.")
                }
            }
            REQUEST_PICK -> {
                if (resultCode == RESULT_OK) {
                    userPhotoPath = data?.data
                    displaySelectedImage(userPhotoPath)
                }
            }
        }
    }

    private fun displaySelectedImage(imageUri: Uri?) {
        binding.materialCardView.removeAllViews()
        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            setImageURI(imageUri)
            scaleType = ImageView.ScaleType.CENTER_CROP
            adjustViewBounds = true
        }
        binding.materialCardView.addView(imageView)
    }

    private fun validateFields(): Boolean {
        var isValid = true

        if (binding.signupEmail.editText?.text.toString().isEmpty() ||
            !android.util.Patterns.EMAIL_ADDRESS.matcher(binding.signupEmail.editText?.text.toString()).matches()
        ) {
            binding.signupEmail.error = getString(R.string.mail_error_label)
            isValid = false
        } else binding.signupEmail.isErrorEnabled = false

        if (binding.signupPass.editText?.text.toString().isEmpty()) {
            binding.signupPass.error = getString(R.string.error_pass_label)
            isValid = false
        } else binding.signupPass.isErrorEnabled = false

        if (binding.signupName.editText?.text.toString().isEmpty()) {
            binding.signupName.error = getString(R.string.error_name_label)
            isValid = false
        } else binding.signupName.isErrorEnabled = false

        if (binding.signupPhone.editText?.text.toString().isEmpty()) {
            binding.signupPhone.error = getString(R.string.error_phone_label)
            isValid = false
        } else binding.signupPhone.isErrorEnabled = false

        if (userPhotoPath == null) {
            alerts.showErrorDialog("Error", "Es obligatorio seleccionar una foto de perfil.")
            isValid = false
        }

        return isValid
    }

    private fun disableFields() {
        binding.registerLoader.visibility = LinearLayout.VISIBLE
        binding.signupName.isEnabled = false
        binding.signupPass.isEnabled = false
        binding.signupEmail.isEnabled = false
        binding.signupButton.isEnabled = false
        binding.signupPhone.isEnabled = false
        binding.regGalleryBtn.isEnabled = false
        binding.regPhotoBtn.isEnabled = false
    }

    private fun enableFields() {
        binding.registerLoader.visibility = LinearLayout.GONE
        binding.signupName.isEnabled = true
        binding.signupPass.isEnabled = true
        binding.signupEmail.isEnabled = true
        binding.signupButton.isEnabled = true
        binding.signupPhone.isEnabled = true
        binding.regGalleryBtn.isEnabled = true
        binding.regPhotoBtn.isEnabled = true
    }
}