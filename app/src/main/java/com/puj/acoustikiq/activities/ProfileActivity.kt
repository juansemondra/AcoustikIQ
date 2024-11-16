package com.puj.acoustikiq.activities

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.puj.acoustikiq.R
import com.puj.acoustikiq.databinding.ActivityProfileBinding
import java.io.File
import java.util.Date

class ProfileActivity : AuthorizedActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val storage = FirebaseStorage.getInstance()
    private val refProfileImg = storage.reference.child("users/${currentUser?.uid}/profile.jpg")

    private val PERM_CAMERA_CODE = 101
    private val REQUEST_IMAGE_CAPTURE = 10
    private val PERM_GALLERY_GROUP_CODE = 202
    private val REQUEST_PICK = 3
    private lateinit var outputPath: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.profilePhotoBtn.setOnClickListener { requestCameraPermission() }
        binding.profileGalleryBtn.setOnClickListener { startGallery() }
        binding.profileButton.setOnClickListener { updateProfile() }
        binding.backButton.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        loadUserData { populateProfileData() }
    }

    private fun populateProfileData() {
        binding.profileName.editText?.setText(user.name)
        binding.profilePhone.editText?.setText(user.phone)
        Glide.with(this)
            .load(refProfileImg)
            .centerCrop()
            .placeholder(R.drawable.baseline_face_24)
            .into(binding.profileImage)
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                takePhoto()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                alerts.indefiniteSnackbar(binding.root, "El permiso de Cámara es necesario para usar esta actividad.")
            }
            else -> {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), PERM_CAMERA_CODE)
            }
        }
    }

    private fun startGallery() {
        val intentPick = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        startActivityForResult(intentPick, REQUEST_PICK)
    }

    private fun takePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val imageFileName = "${Date()}.jpg"
        val imageFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), imageFileName)
        outputPath = FileProvider.getUriForFile(this, "com.puj.acoustikiq.fileprovider", imageFile)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputPath)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            alerts.showErrorDialog("Error", e.localizedMessage ?: "No se pudo abrir la cámara.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            Glide.with(this).clear(binding.profileImage)
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    alerts.shortSimpleSnackbar(binding.root, "Foto tomada correctamente.")
                }
                REQUEST_PICK -> {
                    outputPath = data?.data ?: outputPath
                    alerts.shortSimpleSnackbar(binding.root, "Imagen seleccionada correctamente.")
                }
            }
            Glide.with(this)
                .load(outputPath)
                .centerCrop()
                .placeholder(R.drawable.baseline_face_24)
                .into(binding.profileImage)
        }
    }

    private fun validate(): Boolean {
        val nameValid = binding.profileName.editText?.text?.isNotEmpty() == true
        val phoneValid = binding.profilePhone.editText?.text?.isNotEmpty() == true

        binding.profileName.error = if (!nameValid) "El nombre es requerido." else null
        binding.profilePhone.error = if (!phoneValid) "El teléfono es requerido." else null

        return nameValid && phoneValid
    }

    private fun updateProfile() {
        if (validate()) {
            disableFields()
            user.name = binding.profileName.editText?.text.toString()
            user.phone = binding.profilePhone.editText?.text.toString()
            refData.setValue(user).addOnCompleteListener {
                uploadProfileImageIfNeeded()
            }.addOnFailureListener {
                enableFields()
                alerts.showErrorDialog("Error", "No se pudo actualizar el perfil.")
            }
        }
    }

    private fun uploadProfileImageIfNeeded() {
        if (this::outputPath.isInitialized) {
            refProfileImg.putFile(outputPath).addOnCompleteListener {
                enableFields()
                alerts.shortSimpleSnackbar(binding.root, "Perfil actualizado correctamente.")
            }.addOnFailureListener {
                enableFields()
                alerts.showErrorDialog("Error", "No se pudo actualizar la foto de perfil.")
            }
        } else {
            enableFields()
            alerts.shortSimpleSnackbar(binding.root, "Perfil actualizado correctamente.")
        }
    }

    private fun disableFields() {
        binding.profileName.isEnabled = false
        binding.profilePass.isEnabled = false
        binding.profilePhone.isEnabled = false
        binding.profilePhotoBtn.isEnabled = false
        binding.profileGalleryBtn.isEnabled = false
        binding.profileButton.isEnabled = false
    }

    private fun enableFields() {
        binding.profileName.isEnabled = true
        binding.profilePass.isEnabled = true
        binding.profilePhone.isEnabled = true
        binding.profilePhotoBtn.isEnabled = true
        binding.profileGalleryBtn.isEnabled = true
        binding.profileButton.isEnabled = true
    }
}