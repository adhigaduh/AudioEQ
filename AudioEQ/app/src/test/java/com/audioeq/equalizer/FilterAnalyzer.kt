package com.audioeq.equalizer

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object FilterAnalyzer {
    
    fun calculateFrequencyResponse(
        b0: Double, b1: Double, b2: Double,
        a1: Double, a2: Double,
        sampleRate: Float,
        frequency: Float
    ): Double {
        val omega = 2.0 * PI * frequency / sampleRate
        val cosOmega = cos(omega)
        val sinOmega = sin(omega)
        
        val realNumerator = b0 + b1 * cosOmega + b2 * cos(2 * omega)
        val imagNumerator = -(b1 * sinOmega + b2 * sin(2 * omega))
        
        val realDenominator = 1.0 + a1 * cosOmega + a2 * cos(2 * omega)
        val imagDenominator = -(a1 * sinOmega + a2 * sin(2 * omega))
        
        val magnitudeNumerator = sqrt(realNumerator * realNumerator + imagNumerator * imagNumerator)
        val magnitudeDenominator = sqrt(realDenominator * realDenominator + imagDenominator * imagDenominator)
        
        return magnitudeNumerator / magnitudeDenominator
    }
    
    fun calculateMagnitudeDb(magnitude: Double): Double {
        return 20.0 * kotlin.math.log10(magnitude)
    }
    
    fun generateSineWave(
        frequency: Float,
        sampleRate: Float,
        numSamples: Int,
        amplitude: Double = 1.0
    ): DoubleArray {
        return DoubleArray(numSamples) { i ->
            amplitude * sin(2.0 * PI * frequency * i / sampleRate)
        }
    }
    
    fun calculateRms(samples: DoubleArray): Double {
        val sumOfSquares = samples.sumOf { it * it }
        return sqrt(sumOfSquares / samples.size)
    }
    
    fun measureFrequencyResponse(
        filter: BiquadFilter,
        targetFrequency: Float,
        sampleRate: Float,
        numSamples: Int = 8192
    ): Double {
        val inputSignal = generateSineWave(targetFrequency, sampleRate, numSamples)
        val outputSignal = inputSignal.map { filter.process(it) }.toDoubleArray()
        
        val warmupSamples = numSamples / 4
        val outputRms = calculateRms(outputSignal.copyOfRange(warmupSamples, numSamples))
        val inputRms = calculateRms(inputSignal.copyOfRange(warmupSamples, numSamples))
        
        return outputRms / inputRms
    }
}
