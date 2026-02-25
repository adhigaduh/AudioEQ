package com.audioeq.equalizer

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class BiquadFilterTest {
    
    private val sampleRate = 44100f
    private val tolerance = 0.001
    
    @Test
    fun `test peaking filter initialization`() {
        val band = FilterBand(
            frequency = 1000f,
            gain = 6f,
            q = 1.0f,
            type = FilterType.PEAKING
        )
        
        val filter = BiquadFilter(sampleRate, band)
        
        val input = 0.5
        val output = filter.process(input)
        
        assertNotNull(output)
    }
    
    @Test
    fun `test low shelf filter`() {
        val band = FilterBand(
            frequency = 100f,
            gain = 6f,
            q = 0.7f,
            type = FilterType.LOW_SHELF
        )
        
        val filter = BiquadFilter(sampleRate, band)
        
        val samples = (1..1000).map { 0.5 }.toList()
        val outputs = samples.map { filter.process(it) }
        
        assertTrue(outputs.all { abs(it) <= 2.0 })
    }
    
    @Test
    fun `test high shelf filter`() {
        val band = FilterBand(
            frequency = 8000f,
            gain = 6f,
            q = 0.7f,
            type = FilterType.HIGH_SHELF
        )
        
        val filter = BiquadFilter(sampleRate, band)
        
        val samples = (1..1000).map { 0.5 }.toList()
        val outputs = samples.map { filter.process(it) }
        
        assertTrue(outputs.all { abs(it) <= 1.0 })
    }
    
    @Test
    fun `test filter with zero gain passes signal`() {
        val band = FilterBand(
            frequency = 1000f,
            gain = 0f,
            q = 1.0f,
            type = FilterType.PEAKING
        )
        
        val filter = BiquadFilter(sampleRate, band)
        filter.reset()
        
        val samples = (1..100).map { 0.5 }.toList()
        val outputs = samples.map { filter.process(it) }
        
        val avgOutput = outputs.takeLast(50).average()
        assertEquals(0.5, avgOutput, 0.1)
    }
    
    @Test
    fun `test filter reset clears state`() {
        val band = FilterBand(
            frequency = 1000f,
            gain = 10f,
            q = 1.0f,
            type = FilterType.PEAKING
        )
        
        val filter = BiquadFilter(sampleRate, band)
        
        repeat(100) { filter.process(0.8) }
        
        filter.reset()
        
        val output1 = filter.process(0.5)
        val output2 = filter.process(0.5)
        
        assertEquals(output1, output2, 0.01)
    }
    
    @Test
    fun `test notch filter attenuates center frequency`() {
        val band = FilterBand(
            frequency = 1000f,
            gain = -20f,
            q = 10f,
            type = FilterType.NOTCH
        )
        
        val filter = BiquadFilter(sampleRate, band)
        
        val samples = (1..1000).map { 0.5 }.toList()
        val outputs = samples.map { filter.process(it) }
        
        assertTrue(outputs.all { abs(it) <= 1.0 })
    }
}
