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
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.LineGraphSeries
import com.jjoe64.graphview.series.DataPoint
import com.puj.acoustikiq.activities.MainActivity.Companion.REQUEST_CODE_MIC_PERMISSION
import com.puj.acoustikiq.databinding.FragmentFftBinding
import kotlin.math.*
import java.util.LinkedList
import com.puj.acoustikiq.util.Complex
import com.puj.acoustikiq.util.FFTProcessor

class FFTFragment : Fragment() {

    private var fftBinding: FragmentFftBinding? = null
    private val binding get() = fftBinding!!

    private lateinit var graph: GraphView
    private lateinit var audioRecord: AudioRecord
    private val sampleRate = 44100
    private val bufferSize = 2048
    private val fftSize = 2048
    private var fftProcessor = FFTProcessor(fftSize)

    var isRecording = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fftBinding = FragmentFftBinding.inflate(inflater, container, false)
        graph = binding.graph

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

        val audioBuffer = ShortArray(bufferSize)
        audioRecord.startRecording()
        isRecording = true

        Thread {
            while (isRecording) {
                val readCount = audioRecord.read(audioBuffer, 0, bufferSize)

                if (readCount > 0) {
                    val complexBuffer = Array(bufferSize) { i ->
                        Complex(audioBuffer[i].toDouble(), 0.0)
                    }

                    val windowedSignal = fftProcessor.applyHannWindow(complexBuffer)
                    val fftResult = fftProcessor.fft(windowedSignal)

                    val magnitudes = fftProcessor.dbConverter(fftResult)
                    val frequencies = fftProcessor.frequencyConverter(fftResult, sampleRate)

                    activity.runOnUiThread {
                        updateGraph(frequencies, magnitudes)
                    }

                    complexBuffer.fill(Complex(0.0, 0.0))
                    audioBuffer.fill(0)
                }
            }

            audioRecord.release()
        }.start()
    }


    private fun stopRecording() {
        if (isRecording && ::audioRecord.isInitialized) {
            isRecording = false
            audioRecord.stop()
            audioRecord.release()
        }
    }

    private fun setupGraph() {
        graph.viewport.isScalable = true
        graph.viewport.isScrollable = true
        graph.viewport.setMinY(-90.0)
        graph.viewport.setMaxY(0.0)
        graph.viewport.isYAxisBoundsManual = true

        graph.viewport.setMinX(20.0)
        graph.viewport.setMaxX(20000.0)
        graph.viewport.isXAxisBoundsManual = true

        graph.gridLabelRenderer.horizontalAxisTitle = "Frequency (Hz)"
        graph.gridLabelRenderer.verticalAxisTitle = "Magnitude (dB)"
        graph.gridLabelRenderer.numHorizontalLabels = 5
        graph.gridLabelRenderer.numVerticalLabels = 4

        graph.gridLabelRenderer.labelFormatter = CustomLogarithmicLabelFormatter()
    }

    private fun updateGraph(frequencies: DoubleArray, magnitudesInDb: DoubleArray) {
        val series = LineGraphSeries<DataPoint>()

        val dataPoints = frequencies.indices
            .map { i -> frequencies[i] to magnitudesInDb[i] }
            .filter { it.first in 20.0..20000.0 }
            .sortedBy { it.first }

        for ((frequency, magnitude) in dataPoints) {
            println("Frecuencia: $frequency Hz, Magnitud: $magnitude dB")
            series.appendData(DataPoint(frequency, magnitude), true, frequencies.size)  // Sin log10
        }

        graph.removeAllSeries()
        graph.addSeries(series)
    }

    class CustomLogarithmicLabelFormatter : com.jjoe64.graphview.DefaultLabelFormatter() {
        override fun formatLabel(value: Double, isValueX: Boolean): String {
            if (isValueX) {
                val realValue = 10.0.pow(value)
                return when {
                    realValue >= 1000 -> "${(realValue / 1000).toInt()}k"
                    else -> realValue.toInt().toString()
                }
            }
            return super.formatLabel(value, isValueX)
        }
    }


}