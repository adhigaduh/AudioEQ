package com.audioeq.equalizer

import org.junit.Assert.*
import org.junit.Test

class PerformanceBenchmarkTest {
    
    private val sampleRate = 44100f
    
    @Test
    fun `single filter processes samples efficiently`() {
        val band = FilterBand(1000f, 6f, 1.0f, FilterType.PEAKING)
        val filter = BiquadFilter(sampleRate, band)
        
        val iterations = 100000
        val startTime = System.nanoTime()
        
        repeat(iterations) {
            filter.process(0.5)
        }
        
        val durationMs = (System.nanoTime() - startTime) / 1_000_000.0
        val samplesPerMs = iterations / durationMs
        
        assertTrue(
            "Should process at least 100,000 samples per ms, got $samplesPerMs",
            samplesPerMs > 100_000
        )
    }
    
    @Test
    fun `equalizer processes buffer efficiently`() {
        val equalizer = ParametricEqualizer(sampleRate, 10)
        equalizer.setBandGain(0, 6f)
        equalizer.setBandGain(4, -3f)
        equalizer.setBandGain(9, 4f)
        
        val bufferSize = 4096
        val inputBuffer = ShortArray(bufferSize) { 16384 }
        val outputBuffer = ShortArray(bufferSize)
        
        val iterations = 100
        val startTime = System.nanoTime()
        
        repeat(iterations) {
            equalizer.processBuffer(inputBuffer, outputBuffer)
        }
        
        val durationMs = (System.nanoTime() - startTime) / 1_000_000.0
        val totalSamples = bufferSize * iterations
        val samplesPerMs = totalSamples / durationMs
        
        assertTrue(
            "Should process at least 1,000,000 samples per ms, got $samplesPerMs",
            samplesPerMs > 1_000_000
        )
    }
    
    @Test
    fun `equalizer real-time capable`() {
        val equalizer = ParametricEqualizer(sampleRate, 10)
        
        val bufferSize = 1024
        val bufferDurationMs = (bufferSize.toDouble() / sampleRate) * 1000
        
        val inputBuffer = ShortArray(bufferSize) { 16384 }
        val outputBuffer = ShortArray(bufferSize)
        
        val startTime = System.nanoTime()
        equalizer.processBuffer(inputBuffer, outputBuffer)
        val processingTimeMs = (System.nanoTime() - startTime) / 1_000_000.0
        
        assertTrue(
            "Processing time ($processingTimeMs ms) should be less than buffer duration ($bufferDurationMs ms)",
            processingTimeMs < bufferDurationMs * 0.5
        )
    }
    
    @Test
    fun `preset application is fast`() {
        val equalizer = ParametricEqualizer(sampleRate, 10)
        
        val iterations = 1000
        val startTime = System.nanoTime()
        
        repeat(iterations) {
            equalizer.applyPreset(ParametricEqualizer.PRESET_BASS_BOOST)
        }
        
        val durationMs = (System.nanoTime() - startTime) / 1_000_000.0
        val avgTimeMs = durationMs / iterations
        
        assertTrue(
            "Average preset application time ($avgTimeMs ms) should be less than 1ms",
            avgTimeMs < 1.0
        )
    }
    
    @Test
    fun `filter creation is fast`() {
        val iterations = 1000
        val startTime = System.nanoTime()
        
        repeat(iterations) {
            BiquadFilter(sampleRate, FilterBand(1000f, 6f, 1.0f, FilterType.PEAKING))
        }
        
        val durationMs = (System.nanoTime() - startTime) / 1_000_000.0
        val avgTimeMs = durationMs / iterations
        
        assertTrue(
            "Average filter creation time ($avgTimeMs ms) should be less than 0.1ms",
            avgTimeMs < 0.1
        )
    }
    
    @Test
    fun `equalizer with all bands processes efficiently`() {
        val equalizer = ParametricEqualizer(sampleRate, 10)
        
        equalizer.setBandGain(0, 6f)
        equalizer.setBandGain(1, 4f)
        equalizer.setBandGain(2, 2f)
        equalizer.setBandGain(3, -2f)
        equalizer.setBandGain(4, 0f)
        equalizer.setBandGain(5, 3f)
        equalizer.setBandGain(6, -1f)
        equalizer.setBandGain(7, 5f)
        equalizer.setBandGain(8, 4f)
        equalizer.setBandGain(9, 6f)
        
        val bufferSize = 4096
        val inputBuffer = ShortArray(bufferSize) { 16384 }
        val outputBuffer = ShortArray(bufferSize)
        
        val iterations = 100
        val startTime = System.nanoTime()
        
        repeat(iterations) {
            equalizer.processBuffer(inputBuffer, outputBuffer)
        }
        
        val durationMs = (System.nanoTime() - startTime) / 1_000_000.0
        
        assertTrue(
            "Processing 100 buffers should take less than 50ms, took $durationMs ms",
            durationMs < 50
        )
    }
    
    @Test
    fun `filter reset is fast`() {
        val filter = BiquadFilter(sampleRate, FilterBand(1000f, 6f, 1.0f, FilterType.PEAKING))
        
        val iterations = 10000
        val startTime = System.nanoTime()
        
        repeat(iterations) {
            filter.reset()
        }
        
        val durationMs = (System.nanoTime() - startTime) / 1_000_000.0
        
        assertTrue(
            "10000 resets should take less than 10ms, took $durationMs ms",
            durationMs < 10
        )
    }
}
