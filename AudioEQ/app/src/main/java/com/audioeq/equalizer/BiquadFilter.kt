package com.audioeq.equalizer

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

data class FilterBand(
    val frequency: Float,
    val gain: Float,
    val q: Float,
    val type: FilterType
)

enum class FilterType {
    LOW_SHELF,
    HIGH_SHELF,
    PEAKING,
    LOW_PASS,
    HIGH_PASS,
    NOTCH
}

class BiquadFilter(
    sampleRate: Float,
    private val band: FilterBand
) {
    private var b0 = 0.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0
    
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0
    
    init {
        calculateCoefficients(sampleRate, band)
    }
    
    private fun calculateCoefficients(sampleRate: Float, band: FilterBand) {
        val omega = 2.0 * PI * band.frequency / sampleRate
        val sinOmega = kotlin.math.sin(omega)
        val cosOmega = kotlin.math.cos(omega)
        val alpha = sinOmega / (2.0 * band.q)
        val a = 10.0.pow(band.gain / 40.0)
        
        when (band.type) {
            FilterType.PEAKING -> {
                val b0Val = 1.0 + alpha * a
                val b1Val = -2.0 * cosOmega
                val b2Val = 1.0 - alpha * a
                val a0Val = 1.0 + alpha / a
                
                b0 = b0Val / a0Val
                b1 = b1Val / a0Val
                b2 = b2Val / a0Val
                a1 = (-2.0 * cosOmega) / a0Val
                a2 = (1.0 - alpha / a) / a0Val
            }
            FilterType.LOW_SHELF -> {
                val twoSqrtA = 2.0 * sqrt(a) * alpha
                val a0Val = (a + 1.0) + (a - 1.0) * cosOmega + twoSqrtA
                
                b0 = a * ((a + 1.0) - (a - 1.0) * cosOmega + twoSqrtA) / a0Val
                b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosOmega) / a0Val
                b2 = a * ((a + 1.0) - (a - 1.0) * cosOmega - twoSqrtA) / a0Val
                a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cosOmega) / a0Val
                a2 = ((a + 1.0) + (a - 1.0) * cosOmega - twoSqrtA) / a0Val
            }
            FilterType.HIGH_SHELF -> {
                val twoSqrtA = 2.0 * sqrt(a) * alpha
                val a0Val = (a + 1.0) - (a - 1.0) * cosOmega + twoSqrtA
                
                b0 = a * ((a + 1.0) + (a - 1.0) * cosOmega + twoSqrtA) / a0Val
                b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosOmega) / a0Val
                b2 = a * ((a + 1.0) + (a - 1.0) * cosOmega - twoSqrtA) / a0Val
                a1 = 2.0 * ((a - 1.0) - (a + 1.0) * cosOmega) / a0Val
                a2 = ((a + 1.0) - (a - 1.0) * cosOmega - twoSqrtA) / a0Val
            }
            FilterType.LOW_PASS -> {
                val a0Val = 1.0 + alpha
                
                b0 = (1.0 - cosOmega) / 2.0 / a0Val
                b1 = (1.0 - cosOmega) / a0Val
                b2 = (1.0 - cosOmega) / 2.0 / a0Val
                a1 = (-2.0 * cosOmega) / a0Val
                a2 = (1.0 - alpha) / a0Val
            }
            FilterType.HIGH_PASS -> {
                val a0Val = 1.0 + alpha
                
                b0 = (1.0 + cosOmega) / 2.0 / a0Val
                b1 = -(1.0 + cosOmega) / a0Val
                b2 = (1.0 + cosOmega) / 2.0 / a0Val
                a1 = (-2.0 * cosOmega) / a0Val
                a2 = (1.0 - alpha) / a0Val
            }
            FilterType.NOTCH -> {
                val a0Val = 1.0 + alpha
                
                b0 = 1.0
                b1 = -2.0 * cosOmega
                b2 = 1.0
                a1 = -2.0 * cosOmega / a0Val
                a2 = (1.0 - alpha) / a0Val
            }
        }
    }
    
    fun process(sample: Double): Double {
        val output = b0 * sample + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        
        x2 = x1
        x1 = sample
        y2 = y1
        y1 = output
        
        return output
    }
    
    fun reset() {
        x1 = 0.0
        x2 = 0.0
        y1 = 0.0
        y2 = 0.0
    }
}
