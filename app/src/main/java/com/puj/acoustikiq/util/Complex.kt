package com.puj.acoustikiq.util

import com.puj.acoustikiq.fragments.FFTFragment
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Complex(var real: Double, var imaginary: Double) {
    operator fun plus(other: Complex) = Complex(real + other.real, imaginary + other.imaginary)
    operator fun minus(other: Complex) = Complex(real - other.real, imaginary - other.imaginary)
    operator fun times(other: Complex): Complex {
        return Complex(
            real * other.real - imaginary * other.imaginary,
            real * other.imaginary + imaginary * other.real
        )
    }
    fun magnitude(): Double {
        return kotlin.math.sqrt(real * real + imaginary * imaginary)
    }

    companion object {
        fun polar(r: Double, theta: Double): Complex {
            return Complex(r * cos(theta), r * sin(theta))
        }
    }

    fun abs() = sqrt(real * real + imaginary * imaginary)
    override fun toString() = "$real + ${imaginary}i"
}