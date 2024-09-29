package com.puj.acoustikiq.util

import kotlin.random.Random

class PinkNoise {

    private var b: DoubleArray = DoubleArray(7) { 0.0 } // Coeficientes para el filtro

    /**
     * Genera un buffer de ruido rosa en forma de números complejos
     * @param bufferSize Tamaño del buffer que se desea generar
     * @return Array de números complejos representando el ruido rosa
     */
    fun generate(bufferSize: Int): Array<Complex> {
        val pinkNoise = Array(bufferSize) { Complex(0.0, 0.0) }

        for (i in 0 until bufferSize) {
            val whiteNoise = Random.nextDouble(-1.0, 1.0)
            val pinkSample = generatePinkSample(whiteNoise)

            pinkNoise[i].real = pinkSample // Solo parte real (no componente imaginaria)
        }

        return pinkNoise
    }

    /**
     * Aplica el filtro de suavizado espectral para generar una muestra de ruido rosa
     * @param whiteNoise Entrada de ruido blanco
     * @return Muestra de ruido rosa generada
     */
    private fun generatePinkSample(whiteNoise: Double): Double {
        b[0] = 0.99886 * b[0] + whiteNoise * 0.0555179
        b[1] = 0.99332 * b[1] + whiteNoise * 0.0750759
        b[2] = 0.96900 * b[2] + whiteNoise * 0.1538520
        b[3] = 0.86650 * b[3] + whiteNoise * 0.3104856
        b[4] = 0.55000 * b[4] + whiteNoise * 0.5329522
        b[5] = -0.7616 * b[5] - whiteNoise * 0.0168980

        return b.sum() + whiteNoise * 0.5362
    }
}