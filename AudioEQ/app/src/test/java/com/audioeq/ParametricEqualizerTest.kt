package com.audioeq.equalizer

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ParametricEqualizerTest {
    
    private lateinit var equalizer: ParametricEqualizer
    private val sampleRate = 44100f
    
    @Before
    fun setup() {
        equalizer = ParametricEqualizer(sampleRate, 10)
    }
    
    @Test
    fun `test equalizer initialization`() {
        val bands = equalizer.getBands()
        
        assertEquals(10, bands.size)
        assertTrue(bands.all { it.gain == 0f })
    }
    
    @Test
    fun `test set band gain`() {
        equalizer.setBandGain(0, 6f)
        
        val bands = equalizer.getBands()
        assertEquals(6f, bands[0].gain)
    }
    
    @Test
    fun `test set band frequency`() {
        equalizer.setBandFrequency(0, 50f)
        
        val bands = equalizer.getBands()
        assertEquals(50f, bands[0].frequency)
    }
    
    @Test
    fun `test set band Q`() {
        equalizer.setBandQ(0, 2.0f)
        
        val bands = equalizer.getBands()
        assertEquals(2.0f, bands[0].q)
    }
    
    @Test
    fun `test invalid band index ignored`() {
        val originalBands = equalizer.getBands()
        
        equalizer.setBandGain(-1, 6f)
        equalizer.setBandGain(100, 6f)
        
        val bands = equalizer.getBands()
        assertEquals(originalBands, bands)
    }
    
    @Test
    fun `test apply preset`() {
        equalizer.applyPreset(ParametricEqualizer.PRESET_BASS_BOOST)
        
        val bands = equalizer.getBands()
        assertTrue(bands[0].gain > 0)
        assertTrue(bands[1].gain > 0)
    }
    
    @Test
    fun `test process buffer`() {
        val inputBuffer = ShortArray(1024) { 16384 }
        val outputBuffer = ShortArray(1024)
        
        equalizer.processBuffer(inputBuffer, outputBuffer)
        
        assertTrue(outputBuffer.all { it != 0.toShort() })
    }
    
    @Test
    fun `test disabled equalizer passes signal through`() {
        equalizer.isEnabled = false
        
        val input = 0.5
        val output = equalizer.process(input)
        
        assertEquals(input, output, 0.001)
    }
    
    @Test
    fun `test reset clears filter state`() {
        val input = 0.8
        repeat(100) { equalizer.process(input) }
        
        equalizer.reset()
        
        val output1 = equalizer.process(0.5)
        val output2 = equalizer.process(0.5)
        
        assertEquals(output1, output2, 0.001)
    }
    
    @Test
    fun `test all presets valid`() {
        ParametricEqualizer.ALL_PRESETS.forEach { preset ->
            assertEquals(10, preset.bands.size)
            assertTrue(preset.name.isNotEmpty())
        }
    }
}
