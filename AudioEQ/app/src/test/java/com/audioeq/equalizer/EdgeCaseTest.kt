package com.audioeq.equalizer

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class EdgeCaseTest {
    
    private val sampleRate = 44100f
    
    @Test
    fun `filter handles zero input`() {
        val band = FilterBand(1000f, 6f, 1.0f, FilterType.PEAKING)
        val filter = BiquadFilter(sampleRate, band)
        
        repeat(100) {
            val output = filter.process(0.0)
            assertEquals(0.0, output, 0.0001)
        }
    }
    
    @Test
    fun `filter handles maximum input`() {
        val band = FilterBand(1000f, 0f, 1.0f, FilterType.PEAKING)
        val filter = BiquadFilter(sampleRate, band)
        
        val output = filter.process(1.0)
        assertTrue(abs(output) <= 2.0)
    }
    
    @Test
    fun `filter handles negative input`() {
        val band = FilterBand(1000f, 6f, 1.0f, FilterType.PEAKING)
        val filter = BiquadFilter(sampleRate, band)
        
        val output = filter.process(-1.0)
        assertTrue(abs(output) <= 2.0)
    }
    
    @Test
    fun `filter handles very high frequency`() {
        val band = FilterBand(20000f, 6f, 1.0f, FilterType.PEAKING)
        val filter = BiquadFilter(sampleRate, band)
        
        val output = filter.process(0.5)
        assertTrue(abs(output) <= 2.0)
    }
    
    @Test
    fun `filter handles very low frequency`() {
        val band = FilterBand(20f, 6f, 1.0f, FilterType.PEAKING)
        val filter = BiquadFilter(sampleRate, band)
        
        val output = filter.process(0.5)
        assertTrue(abs(output) <= 2.0)
    }
    
    @Test
    fun `filter handles extreme gain`() {
        val band = FilterBand(1000f, 24f, 1.0f, FilterType.PEAKING)
        val filter = BiquadFilter(sampleRate, band)
        
        val outputs = (1..1000).map { filter.process(0.1) }
        assertTrue(outputs.all { it.isFinite() })
    }
    
    @Test
    fun `filter handles extreme Q`() {
        val band = FilterBand(1000f, 6f, 100.0f, FilterType.PEAKING)
        val filter = BiquadFilter(sampleRate, band)
        
        val outputs = (1..1000).map { filter.process(0.1) }
        assertTrue(outputs.all { it.isFinite() })
    }
    
    @Test
    fun `filter handles very low Q`() {
        val band = FilterBand(1000f, 6f, 0.1f, FilterType.PEAKING)
        val filter = BiquadFilter(sampleRate, band)
        
        val outputs = (1..100).map { filter.process(0.5) }
        assertTrue(outputs.all { it.isFinite() })
    }
    
    @Test
    fun `filter handles alternating input`() {
        val band = FilterBand(1000f, 6f, 1.0f, FilterType.PEAKING)
        val filter = BiquadFilter(sampleRate, band)
        
        val outputs = (1..100).map { i ->
            val input = if (i % 2 == 0) 0.5 else -0.5
            filter.process(input)
        }
        
        assertTrue(outputs.all { it.isFinite() })
    }
    
    @Test
    fun `filter handles impulse input`() {
        val band = FilterBand(1000f, 6f, 1.0f, FilterType.PEAKING)
        val filter = BiquadFilter(sampleRate, band)
        
        filter.process(1.0)
        val decay = (1..100).map { filter.process(0.0) }
        
        assertTrue(decay.all { it.isFinite() })
    }
    
    @Test
    fun `filter handles DC offset`() {
        val band = FilterBand(1000f, 0f, 1.0f, FilterType.HIGH_PASS)
        val filter = BiquadFilter(sampleRate, band)
        
        val outputs = (1..1000).map { filter.process(0.5) }
        
        assertTrue(outputs.all { it.isFinite() })
    }
    
    @Test
    fun `equalizer handles empty buffer`() {
        val equalizer = ParametricEqualizer(sampleRate)
        val inputBuffer = ShortArray(0)
        val outputBuffer = ShortArray(0)
        
        equalizer.processBuffer(inputBuffer, outputBuffer)
        
        assertEquals(0, outputBuffer.size)
    }
    
    @Test
    fun `equalizer handles single sample buffer`() {
        val equalizer = ParametricEqualizer(sampleRate)
        val inputBuffer = shortArrayOf(1000)
        val outputBuffer = ShortArray(1)
        
        equalizer.processBuffer(inputBuffer, outputBuffer)
        
        assertTrue(outputBuffer[0].isFinite())
    }
    
    @Test
    fun `filter remains stable after many samples`() {
        val band = FilterBand(1000f, 12f, 1.0f, FilterType.PEAKING)
        val filter = BiquadFilter(sampleRate, band)
        
        var lastOutput = 0.0
        repeat(100000) {
            lastOutput = filter.process(0.5 * kotlin.math.sin(it * 0.01))
        }
        
        assertTrue(abs(lastOutput) < 10.0)
    }
    
    @Test
    fun `equalizer handles rapid preset changes`() {
        val equalizer = ParametricEqualizer(sampleRate)
        
        repeat(100) {
            equalizer.applyPreset(ParametricEqualizer.PRESET_BASS_BOOST)
            equalizer.applyPreset(ParametricEqualizer.PRESET_TREBLE_BOOST)
        }
        
        val bands = equalizer.getBands()
        assertEquals(10, bands.size)
    }
    
    @Test
    fun `equalizer handles rapid band changes`() {
        val equalizer = ParametricEqualizer(sampleRate)
        
        repeat(100) { i ->
            equalizer.setBandGain(0, (i % 24 - 12).toFloat())
            equalizer.setBandQ(0, 0.1f + (i % 10))
        }
        
        assertTrue(equalizer.getBands().isNotEmpty())
    }
}
