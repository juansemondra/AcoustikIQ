package com.puj.acoustikiq.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.fragment.app.Fragment
import com.puj.acoustikiq.databinding.FragmentPhoneGalleryBinding
import java.io.File

class PhoneGalleryFragment : Fragment() {
    private lateinit var binding: FragmentPhoneGalleryBinding
    private val REQUEST_PICK = 2
    private var fileURI: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhoneGalleryBinding.inflate(inflater, container, false)

        binding.buttonGallery.setOnClickListener {
            startGallery(binding.isPhotoOrVideoSwitchGaleria.isChecked)
        }

        binding.previewGaleria.setOnClickListener {
            if (fileURI != null) {
                returnToGalleryView()
            }
        }

        return binding.root
    }

    private fun startGallery(isVideo: Boolean) {
        val intentPick = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intentPick.addCategory(Intent.CATEGORY_OPENABLE)
        intentPick.type = if (isVideo) "video/*" else "image/*"

        val directoryUri = Uri.parse(
            Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES).path + "/AcoustikIQ")

        intentPick.putExtra(DocumentsContract.EXTRA_INITIAL_URI, directoryUri)
        startActivityForResult(intentPick, REQUEST_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            requestCode == REQUEST_PICK && resultCode == RESULT_OK -> {
                fileURI = data?.data
                displayMedia(fileURI)
            }
        }
    }

    private fun displayMedia(fileURI: Uri?) {
        if (fileURI == null) return

        binding.previewGaleria.removeAllViews()
        val newView =
            if (!binding.isPhotoOrVideoSwitchGaleria.isChecked) ImageView(activity) else VideoView(activity)

        newView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )

        binding.previewGaleria.foregroundGravity = View.TEXT_ALIGNMENT_CENTER

        if (!binding.isPhotoOrVideoSwitchGaleria.isChecked) {
            (newView as ImageView).setImageURI(fileURI)
            newView.scaleType = ImageView.ScaleType.FIT_CENTER
            newView.adjustViewBounds = true
        } else {
            (newView as VideoView).setVideoURI(fileURI)
            newView.setMediaController(MediaController(activity))
            newView.start()
            newView.setOnPreparedListener { mp ->
                mp.isLooping = true
            }
        }

        binding.previewGaleria.addView(newView)

        binding.buttonGallery.visibility = View.GONE
    }

    private fun returnToGalleryView() {
        binding.previewGaleria.removeAllViews()

        binding.buttonGallery.visibility = View.VISIBLE
    }
}