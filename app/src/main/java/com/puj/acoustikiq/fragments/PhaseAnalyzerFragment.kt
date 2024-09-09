package com.puj.acoustikiq.fragments

import android.content.pm.PackageManager
import android.media.AudioRecord
import android.media.AudioFormat
import android.media.MediaRecorder
import android.media.AudioTrack
import android.media.AudioManager
import android.media.AudioAttributes
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
import com.puj.acoustikiq.databinding.FragmentPhaseAnalyzerBinding
import org.jtransforms.fft.DoubleFFT_1D
import kotlin.math.log10
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sin

class PhaseAnalyzerFragment : Fragment() {

    private var phaseAnalyzerBinding: FragmentPhaseAnalyzerBinding? = null
    private val binding get() = phaseAnalyzerBinding!!

    private lateinit var audioRecord: AudioRecord
    private lateinit var audioTrack: AudioTrack

    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    )
    private val audioBuffer = ShortArray(bufferSize)

    private lateinit var pinkNoiseFFT: DoubleArray

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        phaseAnalyzerBinding = FragmentPhaseAnalyzerBinding.inflate(inflater, container, false)

        val graphView = binding.graph

        setupGraph(graphView)
        startAudioProcessing()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        phaseAnalyzerBinding = null
    }

    private fun setupGraph(graphView: GraphView) {
        graphView.viewport.setMinY(-180.0)
        graphView.viewport.setMaxY(180.0)
        graphView.viewport.isYAxisBoundsManual = true

        graphView.viewport.setMinX(log10(63.0))
        graphView.viewport.setMaxX(log10(16000.0))
        graphView.viewport.isXAxisBoundsManual = true

        graphView.gridLabelRenderer.horizontalAxisTitle = "Frequency (Hz)"
        graphView.gridLabelRenderer.verticalAxisTitle = "Phase (Degrees)"

        graphView.gridLabelRenderer.isHorizontalLabelsVisible = true
        graphView.gridLabelRenderer.isVerticalLabelsVisible = true
    }

    private fun startAudioProcessing() {
        generateAndComputePinkNoiseFFT()

        startRecording()
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
                    val micFFTResult = calculateFFT(audioBuffer)
                    val phaseDifference = calculatePhaseDifference(pinkNoiseFFT, micFFTResult)
                    updateGraph(phaseDifference)
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

    private fun calculatePhaseDifference(pinkNoiseFFT: DoubleArray, micFFT: DoubleArray): DoubleArray {
        val phaseDifference = DoubleArray(pinkNoiseFFT.size / 2)

        for (i in phaseDifference.indices) {
            val pinkRealPart = pinkNoiseFFT[2 * i]
            val pinkImagPart = pinkNoiseFFT[2 * i + 1]
            val micRealPart = micFFT[2 * i]
            val micImagPart = micFFT[2 * i + 1]

            val pinkPhase = atan2(pinkImagPart, pinkRealPart) * (180 / Math.PI)
            val micPhase = atan2(micImagPart, micRealPart) * (180 / Math.PI)

            var diff = micPhase - pinkPhase

            if (diff > 180) diff -= 360
            if (diff < -180) diff += 360

            phaseDifference[i] = diff
        }

        return phaseDifference
    }

    private fun updateGraph(phaseData: DoubleArray) {
        val series = LineGraphSeries<DataPoint>()
        for (i in phaseData.indices) {
            val freq = i.toDouble() * sampleRate / phaseData.size
            if (freq >= 63 && freq <= 16000) {
                val phase = phaseData[i]
                series.appendData(DataPoint(log10(freq), phase), true, phaseData.size)
            }
        }
        activity?.runOnUiThread {
            binding.graph.removeAllSeries()
            binding.graph.addSeries(series)
        }
    }
}