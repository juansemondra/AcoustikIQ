package com.puj.acoustikiq.fragments

import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.AudioManager
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
import com.puj.acoustikiq.R
import com.puj.acoustikiq.activities.MainActivity.Companion.REQUEST_CODE_MIC_PERMISSION
import org.jtransforms.fft.DoubleFFT_1D
import kotlin.math.log10
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MagnitudeFragment : Fragment() {

    private lateinit var graphView: GraphView
    private lateinit var audioRecord: AudioRecord
    private lateinit var audioTrack: AudioTrack
    private lateinit var pinkNoiseFFT: DoubleArray  // FFT for pink noise

    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    )
    private val audioBuffer = ShortArray(bufferSize)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_magnitude, container, false)
        graphView = view.findViewById(R.id.magnitude_graph)

        setupGraph()
        generateAndComputePinkNoiseFFT()
        startRecording()

        return view
    }

    private fun setupGraph() {
        graphView.viewport.isScalable = true
        graphView.viewport.isScrollable = true
        graphView.viewport.setMinY(-90.0)
        graphView.viewport.setMaxY(0.0)
        graphView.viewport.isYAxisBoundsManual = true

        graphView.viewport.setMinX(log10(20.0))
        graphView.viewport.setMaxX(log10(20000.0))
        graphView.viewport.isXAxisBoundsManual = true

        graphView.gridLabelRenderer.labelFormatter = CustomLogarithmicLabelFormatter()
        graphView.gridLabelRenderer.horizontalAxisTitle = "Frequency (Hz)"
        graphView.gridLabelRenderer.verticalAxisTitle = "Magnitude Difference (dB)"
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

    private fun generateAndComputePinkNoiseFFT() {
        val pinkNoise = ShortArray(bufferSize)
        for (i in pinkNoise.indices) {
            pinkNoise[i] = (sin(2 * PI * i / sampleRate) * Short.MAX_VALUE).toInt().toShort()
        }

        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack.play()
        audioTrack.write(pinkNoise, 0, pinkNoise.size)

        pinkNoiseFFT = calculateFFT(pinkNoise)
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

        Thread {
            while (true) {
                val readSize = audioRecord.read(audioBuffer, 0, bufferSize)
                if (readSize > 0) {
                    val micInputFFT = calculateFFT(audioBuffer)

                    val magnitudeDifference = calculateMagnitudeDifference(pinkNoiseFFT, micInputFFT)

                    updateGraph(magnitudeDifference)
                }
            }
        }.start()
    }

    private fun calculateFFT(input: ShortArray): DoubleArray {
        val inputSignal = input.map { it.toDouble() }.toDoubleArray()
        val fftData = DoubleArray(inputSignal.size * 2)

        for (i in inputSignal.indices) {
            fftData[2 * i] = inputSignal[i]
            fftData[2 * i + 1] = 0.0
        }

        val fft = DoubleFFT_1D(inputSignal.size.toLong())
        fft.complexForward(fftData)

        return fftData
    }

    private fun calculateMagnitudeDifference(pinkNoiseFFT: DoubleArray, micInputFFT: DoubleArray): DoubleArray {
        val magnitudeDifference = DoubleArray(pinkNoiseFFT.size / 2)

        for (i in magnitudeDifference.indices) {
            val pinkNoiseMagnitude = sqrt(pinkNoiseFFT[2 * i] * pinkNoiseFFT[2 * i] + pinkNoiseFFT[2 * i + 1] * pinkNoiseFFT[2 * i + 1])
            val micInputMagnitude = sqrt(micInputFFT[2 * i] * micInputFFT[2 * i] + micInputFFT[2 * i + 1] * micInputFFT[2 * i + 1])

            magnitudeDifference[i] = 20 * log10(micInputMagnitude / pinkNoiseMagnitude)
        }

        return magnitudeDifference
    }

    private fun updateGraph(magnitudeData: DoubleArray) {
        val series = LineGraphSeries<DataPoint>()
        for (i in magnitudeData.indices) {
            val freq = i.toDouble() * sampleRate / magnitudeData.size
            if (freq >= 20 && freq <= 20000) {
                series.appendData(DataPoint(log10(freq), magnitudeData[i]), true, magnitudeData.size)
            }
        }
        activity?.runOnUiThread {
            graphView.removeAllSeries()
            graphView.addSeries(series)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioRecord.stop()
        audioRecord.release()
        audioTrack.stop()
        audioTrack.release()
    }
}