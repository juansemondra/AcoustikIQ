package com.puj.acoustikiq.util

import kotlin.math.atan2
import kotlin.math.PI
import kotlin.math.abs

class PhaseComparator {

    //RECIBE FASE
    /**
     * Calcula la diferencia de fase entre dos señales (micrófono y ruido rosa)
     * @param micPhase Arreglo de fase de la señal capturada por el micrófono
     * @param noisePhase Arreglo de fase de la señal del ruido rosa
     * @return DoubleArray con la diferencia de fase normalizada entre [-π, π] radianes
     */
    fun calculatePhaseDifference(micPhase: DoubleArray, noisePhase: DoubleArray): DoubleArray {
        if (micPhase.size != noisePhase.size) {
            throw IllegalArgumentException("Ambos arreglos deben tener el mismo tamaño")
        }

        return micPhase.indices.map { i ->
            val phaseDiff = micPhase[i] - noisePhase[i]
            normalizePhase(phaseDiff)
        }.toDoubleArray()
    }

    /**
     * Normaliza la diferencia de fase en el rango [-π, π] radianes
     * @param phaseDiff Diferencia de fase no normalizada
     * @return Diferencia de fase normalizada
     */
    fun normalizePhase(phaseDiff: Double): Double {
        var normalizedPhase = phaseDiff % (2 * PI) // Normaliza en el rango [-2π, 2π]

        if (normalizedPhase > PI) {
            normalizedPhase -= 2 * PI
        } else if (normalizedPhase < -PI) {
            normalizedPhase += 2 * PI
        }

        return normalizedPhase
    }

    //CONVIERTE MAGNITUD Y FASE
    private fun calculatePhaseDifferenceFromMagnitude(pinkNoiseFFT: DoubleArray, micFFT: DoubleArray): DoubleArray {
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

}