package com.puj.acoustikiq.fragments

import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.puj.acoustikiq.activities.MainActivity.Companion.REQUEST_CODE_MIC_PERMISSION
import com.puj.acoustikiq.databinding.FragmentMagnitudeBinding
import com.puj.acoustikiq.util.Complex
import com.puj.acoustikiq.util.FFTProcessor
import com.puj.acoustikiq.util.MagnitudeComparator
import com.puj.acoustikiq.util.PinkNoise
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.log10
import kotlin.math.pow

class MagnitudeFragment : Fragment() {

    private var magnitudeFragment: FragmentMagnitudeBinding? = null
    private val binding get() = magnitudeFragment!!

    private lateinit var chart: LineChart
    private lateinit var audioTrack: AudioTrack
    private lateinit var audioRecord: AudioRecord
    private lateinit var executorService: ExecutorService

    private val sampleRate = 44100
    private val bufferSize = 2048
    private val fftSize = 2048
    private val fftProcessor = FFTProcessor(fftSize)
    private val pinkNoiseGenerator = PinkNoise()
    private val diffMagnitude = MagnitudeComparator()

    private lateinit var pinkNoise: Array<Complex>
    private lateinit var pinkNoiseMagnitudes: DoubleArray
    private lateinit var pinkNoiseFrequencies: DoubleArray
    private lateinit var micMagnitudes: DoubleArray
    private lateinit var micFrequencies: DoubleArray
    private lateinit var magnitudeComparison: DoubleArray
    private lateinit var pinkNoiseShort: ShortArray

    private var isRecording = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        magnitudeFragment = FragmentMagnitudeBinding.inflate(inflater, container, false)
        chart = binding.graph

        setupGraph()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        startRecording()
    }

    override fun onPause() {
        super.onPause()
        stopRecording()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopRecording()
        magnitudeFragment = null
    }


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

        audioTrack = AudioTrack.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioRecord.startRecording()
        audioTrack.play()
        isRecording = true

        executorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            val audioBuffer = ShortArray(bufferSize)
            val complexBuffer = Array(bufferSize) { Complex(0.0, 0.0) }
            val pinkNoise = pinkNoiseGenerator.generate(bufferSize)
            pinkNoiseMagnitudes = fftProcessor.dbConverter(pinkNoise)
            pinkNoiseFrequencies = fftProcessor.frequencyConverter(pinkNoise, sampleRate)
            val pinkNoiseShort = pinkNoise.map {
                val scaledValue = (it.real * 32767).coerceIn(-32768.0, 32767.0).toInt().toShort()
                scaledValue
            }.toShortArray()


            while (isRecording) {
                audioTrack.write(pinkNoiseShort, 0, bufferSize)

                val readCount = audioRecord.read(audioBuffer, 0, bufferSize)
                if (readCount > 0) {
                    for (i in audioBuffer.indices) {
                        complexBuffer[i].real = audioBuffer[i].toDouble()
                        complexBuffer[i].imaginary = 0.0
                    }

                    val windowedSignal = fftProcessor.applyHannWindow(complexBuffer)
                    val fftResult = fftProcessor.fft(windowedSignal)
                    micMagnitudes = fftProcessor.dbConverter(fftResult)
                    micFrequencies = fftProcessor.frequencyConverter(fftResult, sampleRate)

                    val magnitudeComparison = diffMagnitude.compareMagnitudesFFTStyle(micMagnitudes, pinkNoiseMagnitudes)

                    activity.runOnUiThread {
                        updateGraph(micFrequencies, magnitudeComparison)
                    }

                    audioBuffer.fill(0) // Limpiar el búfer de audio
                }
            }

            audioRecord.release()
        }
    }
    
    private fun stopRecording() {
        if (isRecording && ::audioRecord.isInitialized) {
            isRecording = false
            audioRecord.stop()
            audioRecord.release()
        }

    }

    private fun setupGraph() {
        chart.description.isEnabled = false

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setLabelCount(6, true)
        xAxis.axisMinimum = log10(20f)
        xAxis.axisMaximum = log10(20000f)
        xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val freq = 10.0.pow(value.toDouble()).toFloat()
                return when {
                    freq >= 1000 -> "${(freq / 1000).toInt()}k"
                    else -> freq.toInt().toString()
                }
            }
        }
        xAxis.labelRotationAngle = 45f

        val yAxis = chart.axisLeft
        yAxis.axisMinimum = -90f
        yAxis.axisMaximum = 0f

        chart.axisRight.isEnabled = false
    }

    private fun updateGraph(frequencies: DoubleArray, magnitudesInDb: DoubleArray) {
        val dataPoints = frequencies.indices
            .map { i -> Entry(log10(frequencies[i].toFloat()).takeIf { it.isFinite() } ?: 20f, magnitudesInDb[i].toFloat()) }
            .filter { it.x in log10(20f)..log10(20000f) }
            .sortedBy { it.x }

        val dataSet = LineDataSet(dataPoints, "Magnitudes RTA")
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.lineWidth = 2f

        val lineData = LineData(dataSet)
        chart.data = lineData

        chart.invalidate()
    }
}