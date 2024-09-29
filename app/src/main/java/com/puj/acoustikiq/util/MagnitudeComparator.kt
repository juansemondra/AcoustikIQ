package com.puj.acoustikiq.util

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MagnitudeComparator {

    /**
     * Compara las magnitudes de dos señales (micrófono y ruido rosa) usando una estrategia similar a la FFT.
     * @param micMagnitudes Magnitudes de la señal capturada por el micrófono
     * @param noiseMagnitudes Magnitudes del ruido rosa
     * @return DoubleArray con la diferencia en magnitudes entre las señales
     */
    fun compareMagnitudesFFTStyle(
        micMagnitudes: DoubleArray,
        noiseMagnitudes: DoubleArray
    ): DoubleArray {

        if (micMagnitudes.size != noiseMagnitudes.size) {
            throw IllegalArgumentException("Ambos arreglos deben tener el mismo tamaño")
        }

        return fftCompareRecursive(micMagnitudes, noiseMagnitudes)
    }

    private fun fftCompareRecursive(
        micMagnitudes: DoubleArray,
        noiseMagnitudes: DoubleArray
    ): DoubleArray {
        val n = micMagnitudes.size
        if (n == 1) {
            return doubleArrayOf(micMagnitudes[0] - noiseMagnitudes[0])
        }

        val evenMic = micMagnitudes.filterIndexed { index, _ -> index % 2 == 0 }.toDoubleArray()
        val oddMic = micMagnitudes.filterIndexed { index, _ -> index % 2 != 0 }.toDoubleArray()

        val evenNoise = noiseMagnitudes.filterIndexed { index, _ -> index % 2 == 0 }.toDoubleArray()
        val oddNoise = noiseMagnitudes.filterIndexed { index, _ -> index % 2 != 0 }.toDoubleArray()

        val evenDifferences = fftCompareRecursive(evenMic, evenNoise)
        val oddDifferences = fftCompareRecursive(oddMic, oddNoise)

        val result = DoubleArray(n)

        for (k in 0 until n / 2) {
            val twiddleFactor = Complex(cos(2.0 * PI * k / n), -sin(2.0 * PI * k / n))
            val oddTwiddled = Complex(oddDifferences[k], 0.0) * twiddleFactor

            result[k] = evenDifferences[k] + oddTwiddled.real
            result[k + n / 2] = evenDifferences[k] - oddTwiddled.real
        }

        return result
    }
}