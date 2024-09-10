package com.puj.acoustikiq.fragments

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.puj.acoustikiq.databinding.FragmentLevelMeterBinding
import kotlin.math.log10

class LevelMeterFragment : Fragment() {

    private var levelMeterBinding: FragmentLevelMeterBinding? = null
    private val binding get() = levelMeterBinding

    private lateinit var audioRecord: AudioRecord
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private val audioBuffer = ShortArray(bufferSize)

    private var isRecording = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        levelMeterBinding = FragmentLevelMeterBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startRecording()
    }

    override fun onResume() {
        super.onResume()
        if (!isRecording) {
            startRecording()
        }
    }

    override fun onPause() {
        super.onPause()
        stopRecording()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopRecording()
        levelMeterBinding = null
    }

    @SuppressLint("DefaultLocale")
    private fun startRecording() {
        val context = requireContext()

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.RECORD_AUDIO), 200)
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord.startRecording()
        isRecording = true

        Thread {
            while (isRecording) {
                val readSize = audioRecord.read(audioBuffer, 0, bufferSize)
                if (readSize > 0) {
                    val rms = calculateRms(audioBuffer, readSize)
                    val decibels = calculateDecibels(rms)

                    // Update UI on the main thread
                    binding?.let {
                        activity?.runOnUiThread {
                            updateUI(decibels, rms)
                        }
                    }
                }
            }
        }.start()
    }

    private fun stopRecording() {
        if (isRecording) {
            isRecording = false
            audioRecord.stop()
            audioRecord.release()
        }
    }

    private fun updateUI(decibels: Double, rms: Double) {
        val rmsPercent = ((rms / Short.MAX_VALUE) * 100).toInt()

        binding?.decibelText?.text = String.format("%.2f dB", decibels)

        binding?.rmsBar?.progress = rmsPercent

        binding?.integratedLoudnessBar?.progress = rmsPercent

        if (decibels >= 0) {
            binding?.warningBox?.visibility = View.VISIBLE
        } else {
            binding?.warningBox?.visibility = View.GONE
        }
    }

    private fun calculateRms(buffer: ShortArray, size: Int): Double {
        var sum: Long = 0
        for (i in 0 until size) {
            sum += buffer[i] * buffer[i]
        }
        return kotlin.math.sqrt(sum.toDouble() / size)
    }

    private fun calculateDecibels(rms: Double): Double {
        return 20 * log10(rms / Short.MAX_VALUE)
    }
}