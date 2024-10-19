package com.puj.acoustikiq.fragments

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.puj.acoustikiq.databinding.FragmentCamaraBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CamaraFragment : Fragment() {
    private lateinit var binding: FragmentCamaraBinding
    private lateinit var currentFilePath: String
    private val REQUEST_CAPTURE = 1
    private lateinit var fileURI: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCamaraBinding.inflate(inflater, container, false)
        binding.buttonCamera.setOnClickListener {
            dispatchTakePictureIntent()
        }

        return binding.root
    }

    @Throws(IOException::class)
    private fun createImageOrVideoFile(isVideo: Boolean): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), "AcoustikIQ"
        )

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                throw IOException("Failed to create directory")
            }
        }

        return File.createTempFile(
            "${if (isVideo) "MP4" else "IMG"}_${timeStamp}_",
            if (isVideo) ".mp4" else ".jpg",
            mediaStorageDir
        ).apply {
            currentFilePath = absolutePath
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun dispatchTakePictureIntent() {
        Intent(if (binding.isPhotoOrVideoSwitchCamara.isChecked) MediaStore.ACTION_VIDEO_CAPTURE else MediaStore.ACTION_IMAGE_CAPTURE).also { takeIntent ->
            takeIntent.resolveActivity(activity?.packageManager!!).also {
                val routeFile: File? = try {
                    createImageOrVideoFile(binding.isPhotoOrVideoSwitchCamara.isChecked)
                } catch (ex: IOException) {
                    ex.printStackTrace()
                    null
                }
                routeFile?.also { route ->
                    fileURI = FileProvider.getUriForFile(
                        activity?.applicationContext!!,
                        "${activity?.applicationContext?.packageName}.fileprovider",
                        route
                    )
                    takeIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileURI)
                    takeIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 1024 * 1024)
                    startActivityForResult(takeIntent, REQUEST_CAPTURE)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            requestCode == REQUEST_CAPTURE && resultCode == RESULT_OK -> {
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                val file = File(currentFilePath)
                val contentUri = Uri.fromFile(file)
                mediaScanIntent.data = contentUri
                activity?.sendBroadcast(mediaScanIntent)

                binding.previewCamara.removeAllViews()
                val newView =
                    if (!binding.isPhotoOrVideoSwitchCamara.isChecked)
                        ImageView(activity)
                    else
                        VideoView(activity)
                newView.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
                binding.previewCamara.foregroundGravity = View.TEXT_ALIGNMENT_CENTER
                if (!binding.isPhotoOrVideoSwitchCamara.isChecked) {
                    (newView as ImageView).setImageURI(fileURI)
                    newView.scaleType = ImageView.ScaleType.FIT_CENTER
                    newView.adjustViewBounds = true
                } else {
                    (newView as VideoView).setVideoURI(fileURI)
                    newView.foregroundGravity = View.TEXT_ALIGNMENT_CENTER
                    newView.setMediaController(MediaController(activity))
                    newView.start()
                    newView.setOnPreparedListener { mp ->
                        mp.isLooping = true
                    }
                }
                binding.previewCamara.addView(newView)
            }
            REQUEST_CAPTURE == requestCode && resultCode != RESULT_OK -> {
                File(currentFilePath).delete()
            }
        }
    }
}