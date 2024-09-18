package com.puj.acoustikiq.fragments

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.puj.acoustikiq.activities.MainActivity.Companion.REQUEST_CODE_MIC_PERMISSION
import com.puj.acoustikiq.databinding.FragmentLevelMeterBinding
import kotlin.math.log10

class LevelMeterFragment : Fragment() {

    private var levelMeterBinding: FragmentLevelMeterBinding? = null
    private val binding get() = levelMeterBinding!!

    private lateinit var audioRecord: AudioRecord
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private val audioBuffer = ShortArray(bufferSize)

    private var isRecording = false
    private var integratedLoudness = 0.0
    private var bufferFlushInterval = 1000L  // Flush buffer every second
    private var lastFlushTime = System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        levelMeterBinding = FragmentLevelMeterBinding.inflate(inflater, container, false)
        return binding.root
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
        val activity = requireActivity()

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_CODE_MIC_PERMISSION)
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord.state == AudioRecord.STATE_UNINITIALIZED) {
            Log.e("LevelMeterFragment", "AudioRecord initialization failed")
            return
        }

        audioRecord.startRecording()
        isRecording = true

        Thread {
            while (isRecording) {
                val readSize = audioRecord.read(audioBuffer, 0, bufferSize)

                if (readSize <= 0) {
                    Log.e("LevelMeterFragment", "AudioRecord.read() failed with error code: $readSize")
                    continue
                }

                val rms = calculateRms(audioBuffer, readSize)
                val decibels = calculateDecibels(rms)

                integratedLoudness = calculateIntegratedLoudness(rms, integratedLoudness)

                if (isAdded) {
                    Log.d("LevelMeterFragment", "Updating UI: $decibels dB, RMS: $rms")
                    activity.runOnUiThread {
                        updateUI(decibels, integratedLoudness)
                    }
                }

                // Check if it's time to flush the buffer
                if (System.currentTimeMillis() - lastFlushTime >= bufferFlushInterval) {
                    flushBuffer()
                    lastFlushTime = System.currentTimeMillis()
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

    @SuppressLint("DefaultLocale")
    private fun updateUI(decibels: Double, integratedLoudness: Double) {
        val rmsPercent = ((decibels + 60) / 60 * 100).toInt()

        binding.decibelText.text = String.format("%.2f dB", decibels)
        binding.rmsBar.progress = rmsPercent
        binding.integratedLoudnessBar.progress = (integratedLoudness * 100).toInt()

        if (decibels >= 0) {
            binding.warningBox.visibility = View.VISIBLE
        } else {
            binding.warningBox.visibility = View.GONE
        }
    }

    private fun calculateIntegratedLoudness(rms: Double, currentLoudness: Double): Double {
        val alpha = 0.9
        return alpha * currentLoudness + (1 - alpha) * (rms / Short.MAX_VALUE)
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

    private fun flushBuffer() {
        for (i in audioBuffer.indices) {
            audioBuffer[i] = 0
        }
        Log.d("LevelMeterFragment", "Buffer flushed to prevent overflow.")
    }
}