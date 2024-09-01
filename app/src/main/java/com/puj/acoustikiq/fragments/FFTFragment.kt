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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class FFTFragment : Fragment() {

    private var fft_binding: FragmentFftBinding? = null
    private val binding get() = fft_binding!!

    private lateinit var graph: GraphView
    private lateinit var audioRecord: AudioRecord
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private val audioBuffer = ShortArray(bufferSize)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fft_binding = FragmentFftBinding.inflate(inflater, container, false)
        graph = binding.graph
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
        fft_binding = null
    }

    private fun startRecording() {
        val context = requireContext()
        val activity = requireActivity()

        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                REQUEST_CODE_MIC_PERMISSION
            )
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
                    // Adjust the length to the next power of 2
                    val adjustedAudioBuffer = adjustToPowerOf2(audioBuffer, readSize)
                    val complexData = adjustedAudioBuffer.map { Complex(it.toDouble(), 0.0) }.toTypedArray()

                    val fftResult = fft(complexData)

                    activity.runOnUiThread {
                        updateGraph(fftResult)
                    }
                }
            }
        }.start()
    }

    private fun adjustToPowerOf2(buffer: ShortArray, length: Int): ShortArray {
        // Find the next power of 2
        var powerOf2Length = 1
        while (powerOf2Length < length) {
            powerOf2Length *= 2
        }

        // Create a new buffer with the size of the next power of 2
        val adjustedBuffer = ShortArray(powerOf2Length)
        // Copy the original data to the new buffer
        System.arraycopy(buffer, 0, adjustedBuffer, 0, length)

        // The rest of the buffer will be filled with zeros
        return adjustedBuffer
    }

    private fun stopRecording() {
        audioRecord.stop()
        audioRecord.release()
    }

    private fun fft(input: Array<Complex>): Array<Complex> {
        val n = input.size

        if (n == 1) return arrayOf(input[0])

        if (n % 2 != 0) throw IllegalArgumentException("Input array length must be a power of 2")

        val even = fft(input.filterIndexed { index, _ -> index % 2 == 0 }.toTypedArray())
        val odd = fft(input.filterIndexed { index, _ -> index % 2 != 0 }.toTypedArray())

        val result = Array(n) { Complex(0.0, 0.0) }
        for (k in 0 until n / 2) {
            val t = Complex.polar(1.0, -2.0 * PI * k / n) * odd[k]
            result[k] = even[k] + t
            result[k + n / 2] = even[k] - t
        }
        return result
    }

    private fun updateGraph(fftResult: Array<Complex>) {
        val series = LineGraphSeries<DataPoint>()
        for (i in fftResult.indices) {
            val magnitude = fftResult[i].magnitude()
            series.appendData(DataPoint(i.toDouble(), magnitude), true, fftResult.size)
        }
        graph.removeAllSeries()
        graph.addSeries(series)
    }

    data class Complex(val real: Double, val imag: Double) {

        operator fun plus(other: Complex): Complex {
            return Complex(real + other.real, imag + other.imag)
        }

        operator fun minus(other: Complex): Complex {
            return Complex(real - other.real, imag - other.imag)
        }

        operator fun times(other: Complex): Complex {
            return Complex(
                real * other.real - imag * other.imag,
                real * other.imag + imag * other.real
            )
        }

        fun magnitude(): Double {
            return kotlin.math.sqrt(real * real + imag * imag)
        }

        companion object {
            fun polar(r: Double, theta: Double): Complex {
                return Complex(r * cos(theta), r * sin(theta))
            }
        }
    }
}