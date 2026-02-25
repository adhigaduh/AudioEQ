package com.audioeq.equalizer

import org.junit.Assert.*
import org.junit.Test

class FilterBandTest {
    
    @Test
    fun `test filter band creation`() {
        val band = FilterBand(
            frequency = 1000f,
            gain = 6f,
            q = 1.0f,
            type = FilterType.PEAKING
        )
        
        assertEquals(1000f, band.frequency)
        assertEquals(6f, band.gain)
        assertEquals(1.0f, band.q)
        assertEquals(FilterType.PEAKING, band.type)
    }
    
    @Test
    fun `test filter band copy`() {
        val band = FilterBand(1000f, 0f, 1.0f, FilterType.PEAKING)
        val modifiedBand = band.copy(gain = 6f)
        
        assertEquals(1000f, modifiedBand.frequency)
        assertEquals(6f, modifiedBand.gain)
        assertEquals(1.0f, modifiedBand.q)
    }
    
    @Test
    fun `test filter type values`() {
        val types = FilterType.values()
        
        assertEquals(6, types.size)
        assertTrue(FilterType.PEAKING in types)
        assertTrue(FilterType.LOW_SHELF in types)
        assertTrue(FilterType.HIGH_SHELF in types)
        assertTrue(FilterType.LOW_PASS in types)
        assertTrue(FilterType.HIGH_PASS in types)
        assertTrue(FilterType.NOTCH in types)
    }
}
