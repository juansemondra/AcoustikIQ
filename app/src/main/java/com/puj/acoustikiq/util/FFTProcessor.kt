package com.puj.acoustikiq.util
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class FFTProcessor(private val fftSize: Int) {

    companion object {
        var Complex = com.puj.acoustikiq.util.Complex(0.0, 0.0)
    }

    fun fft(x: Array<Complex>): Array<Complex> {
        val n = x.size
        if (n == 1) return arrayOf(x[0])

        val even = Array(n / 2) { i -> x[2 * i] }
        val odd = Array(n / 2) { i -> x[2 * i + 1] }

        val fftEven = fft(even)
        val fftOdd = fft(odd)

        val result = Array(n) { Complex(0.0, 0.0) }
        for (k in 0 until n / 2) {
            val t = Complex(cos(-2 * PI * k / n), sin(-2 * PI * k / n)) * fftOdd[k]
            result[k] = fftEven[k] + t
            result[k + n / 2] = fftEven[k] - t
        }
        return result
    }

    fun applyHannWindow(buffer: Array<Complex>): Array<Complex> {
        val n = buffer.size
        return Array(n) { i ->
            val hannValue = 0.5 * (1 - cos(2 * PI * i / (n - 1)))
            buffer[i] * Complex(hannValue, 0.0)
        }
    }

    fun dbConverter(fftResult: Array<Complex>): DoubleArray {

        val magnitudesInDb = DoubleArray(fftResult.size / 2)
        val maxMagnitude = fftResult.maxOf { it.magnitude() }

        for (i in magnitudesInDb.indices) {
            val magnitude = fftResult[i].magnitude()
            magnitudesInDb[i] = 20 * log10(magnitude / maxMagnitude)
        }
        return magnitudesInDb
    }

    fun frequencyConverter(fftResult: Array<Complex>, sampleRate: Int): DoubleArray {
        val frequencies = DoubleArray(fftResult.size / 2)
        val nyquist = sampleRate / 2.0

        for (i in frequencies.indices) {
            frequencies[i] = i * nyquist / (fftResult.size / 2)
        }

        return frequencies
    }

    fun phaseConverter(fftResult: Array<Complex>): DoubleArray {
        val phases = DoubleArray(fftResult.size / 2)

        for (i in phases.indices) {
            phases[i] = Math.atan2(fftResult[i].imaginary, fftResult[i].real)
        }

        return phases
    }
}