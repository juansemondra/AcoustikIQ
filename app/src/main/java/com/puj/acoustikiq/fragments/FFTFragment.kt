package com.puj.acoustikiq.fragments

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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.puj.acoustikiq.activities.MainActivity.Companion.REQUEST_CODE_MIC_PERMISSION
import com.puj.acoustikiq.databinding.FragmentFftBinding
import com.puj.acoustikiq.util.Complex
import com.puj.acoustikiq.util.FFTProcessor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.log10
import kotlin.math.pow

class FFTFragment : Fragment() {

    private var fftBinding: FragmentFftBinding? = null
    private val binding get() = fftBinding!!

    private lateinit var chart: LineChart
    private lateinit var executorService: ExecutorService
    private lateinit var audioRecord: AudioRecord
    private val sampleRate = 44100
    private val bufferSize = 2048
    private val fftSize = 2048
    private var fftProcessor = FFTProcessor(fftSize)

    private lateinit var magnitudes: DoubleArray
    private lateinit var frequencies: DoubleArray

    var isRecording = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fftBinding = FragmentFftBinding.inflate(inflater, container, false)
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
        fftBinding = null
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

        audioRecord.startRecording()
        isRecording = true

        executorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            val audioBuffer = ShortArray(bufferSize)
            val complexBuffer = Array(bufferSize) { Complex(0.0, 0.0) }

            while (isRecording) {
                val readCount = audioRecord.read(audioBuffer, 0, bufferSize)

                if (readCount > 0) {
                    for (i in 0 until bufferSize) {
                        complexBuffer[i].real = audioBuffer[i].toDouble()
                        complexBuffer[i].imaginary = 0.0
                    }

                    val windowedSignal = fftProcessor.applyHannWindow(complexBuffer)
                    val fftResult = fftProcessor.fft(windowedSignal)

                    magnitudes = fftProcessor.dbConverter(fftResult)
                    frequencies = fftProcessor.frequencyConverter(fftResult, sampleRate)

                    activity.runOnUiThread {
                        updateGraph(frequencies, magnitudes)
                    }

                    audioBuffer.fill(0)
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

    public fun getMag(): DoubleArray {
        return this.magnitudes
    }

    public fun getFrq(): DoubleArray {
        return this.frequencies
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
            .map { i -> Entry(log10(frequencies[i].toFloat()), magnitudesInDb[i].toFloat()) }
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