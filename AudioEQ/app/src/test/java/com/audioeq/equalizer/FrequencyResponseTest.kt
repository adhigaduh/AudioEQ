package com.audioeq.equalizer

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class FrequencyResponseTest {
    
    private val sampleRate = 44100f
    private val tolerance = 0.5
    
    @Test
    fun `peaking filter boosts at center frequency`() {
        val centerFreq = 1000f
        val gainDb = 6f
        val band = FilterBand(centerFreq, gainDb, 1.0f, FilterType.PEAKING)
        val filter = BiquadFilter(sampleRate, band)
        
        val response = FilterAnalyzer.measureFrequencyResponse(filter, centerFreq, sampleRate)
        val responseDb = FilterAnalyzer.calculateMagnitudeDb(response)
        
        assertEquals(gainDb.toDouble(), responseDb, tolerance)
    }
    
    @Test
    fun `peaking filter unity gain far from center`() {
        val centerFreq = 1000f
        val gainDb = 6f
        val band = FilterBand(centerFreq, gainDb, 1.0f, FilterType.PEAKING)
        val filter = BiquadFilter(sampleRate, band)
        
        val farFreq = 20000f
        val response = FilterAnalyzer.measureFrequencyResponse(filter, farFreq, sampleRate)
        val responseDb = FilterAnalyzer.calculateMagnitudeDb(response)
        
        assertEquals(0.0, responseDb, 1.0)
    }
    
    @Test
    fun `low shelf boosts low frequencies`() {
        val shelfFreq = 200f
        val gainDb = 6f
        val band = FilterBand(shelfFreq, gainDb, 0.7f, FilterType.LOW_SHELF)
        val filter = BiquadFilter(sampleRate, band)
        
        val lowFreq = 50f
        val response = FilterAnalyzer.measureFrequencyResponse(filter, lowFreq, sampleRate)
        val responseDb = FilterAnalyzer.calculateMagnitudeDb(response)
        
        assertTrue("Low shelf should boost low frequencies", responseDb > gainDb * 0.5)
    }
    
    @Test
    fun `high shelf boosts high frequencies`() {
        val shelfFreq = 4000f
        val gainDb = 6f
        val band = FilterBand(shelfFreq, gainDb, 0.7f, FilterType.HIGH_SHELF)
        val filter = BiquadFilter(sampleRate, band)
        
        val highFreq = 12000f
        val response = FilterAnalyzer.measureFrequencyResponse(filter, highFreq, sampleRate)
        val responseDb = FilterAnalyzer.calculateMagnitudeDb(response)
        
        assertTrue("High shelf should boost high frequencies", responseDb > gainDb * 0.5)
    }
    
    @Test
    fun `notch filter attenuates center frequency`() {
        val notchFreq = 1000f
        val band = FilterBand(notchFreq, 0f, 10f, FilterType.NOTCH)
        val filter = BiquadFilter(sampleRate, band)
        
        val response = FilterAnalyzer.measureFrequencyResponse(filter, notchFreq, sampleRate)
        val responseDb = FilterAnalyzer.calculateMagnitudeDb(response)
        
        assertTrue("Notch should attenuate at center frequency", responseDb < -20)
    }
    
    @Test
    fun `notch filter passes other frequencies`() {
        val notchFreq = 1000f
        val band = FilterBand(notchFreq, 0f, 10f, FilterType.NOTCH)
        val filter = BiquadFilter(sampleRate, band)
        
        val otherFreq = 100f
        val response = FilterAnalyzer.measureFrequencyResponse(filter, otherFreq, sampleRate)
        val responseDb = FilterAnalyzer.calculateMagnitudeDb(response)
        
        assertEquals(0.0, responseDb, 1.0)
    }
    
    @Test
    fun `low pass attenuates high frequencies`() {
        val cutoffFreq = 1000f
        val band = FilterBand(cutoffFreq, 0f, 0.7f, FilterType.LOW_PASS)
        val filter = BiquadFilter(sampleRate, band)
        
        val lowFreq = 100f
        val lowResponse = FilterAnalyzer.measureFrequencyResponse(filter, lowFreq, sampleRate)
        val lowResponseDb = FilterAnalyzer.calculateMagnitudeDb(lowResponse)
        
        val highFreq = 5000f
        val highResponse = FilterAnalyzer.measureFrequencyResponse(filter, highFreq, sampleRate)
        val highResponseDb = FilterAnalyzer.calculateMagnitudeDb(highResponse)
        
        assertTrue("Low pass should pass low frequencies", lowResponseDb > -3)
        assertTrue("Low pass should attenuate high frequencies", highResponseDb < -6)
    }
    
    @Test
    fun `high pass attenuates low frequencies`() {
        val cutoffFreq = 1000f
        val band = FilterBand(cutoffFreq, 0f, 0.7f, FilterType.HIGH_PASS)
        val filter = BiquadFilter(sampleRate, band)
        
        val lowFreq = 100f
        val lowResponse = FilterAnalyzer.measureFrequencyResponse(filter, lowFreq, sampleRate)
        val lowResponseDb = FilterAnalyzer.calculateMagnitudeDb(lowResponse)
        
        val highFreq = 5000f
        val highResponse = FilterAnalyzer.measureFrequencyResponse(filter, highFreq, sampleRate)
        val highResponseDb = FilterAnalyzer.calculateMagnitudeDb(highResponse)
        
        assertTrue("High pass should attenuate low frequencies", lowResponseDb < -6)
        assertTrue("High pass should pass high frequencies", highResponseDb > -3)
    }
    
    @Test
    fun `negative gain attenuates signal`() {
        val centerFreq = 1000f
        val gainDb = -6f
        val band = FilterBand(centerFreq, gainDb, 1.0f, FilterType.PEAKING)
        val filter = BiquadFilter(sampleRate, band)
        
        val response = FilterAnalyzer.measureFrequencyResponse(filter, centerFreq, sampleRate)
        val responseDb = FilterAnalyzer.calculateMagnitudeDb(response)
        
        assertEquals(gainDb.toDouble(), responseDb, tolerance)
    }
    
    @Test
    fun `Q factor affects bandwidth`() {
        val centerFreq = 1000f
        val gainDb = 6f
        
        val narrowBand = FilterBand(centerFreq, gainDb, 10.0f, FilterType.PEAKING)
        val narrowFilter = BiquadFilter(sampleRate, narrowBand)
        
        val wideBand = FilterBand(centerFreq, gainDb, 0.5f, FilterType.PEAKING)
        val wideFilter = BiquadFilter(sampleRate, wideBand)
        
        val offCenterFreq = 1500f
        
        val narrowResponse = FilterAnalyzer.measureFrequencyResponse(narrowFilter, offCenterFreq, sampleRate)
        val wideResponse = FilterAnalyzer.measureFrequencyResponse(wideFilter, offCenterFreq, sampleRate)
        
        assertTrue(
            "High Q should have narrower bandwidth",
            narrowResponse < wideResponse
        )
    }
}
