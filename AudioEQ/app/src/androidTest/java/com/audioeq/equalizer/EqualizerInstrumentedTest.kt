package com.audioeq.equalizer

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EqualizerInstrumentedTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }
    
    @Test
    fun testEqualizerRealTimeProcessing() {
        val sampleRate = 44100f
        val equalizer = ParametricEqualizer(sampleRate, 10)
        
        equalizer.setBandGain(0, 6f)
        equalizer.setBandGain(9, -3f)
        
        val inputBuffer = ShortArray(4096) { (it % 256 - 128).toShort() }
        val outputBuffer = ShortArray(4096)
        
        val startTime = System.currentTimeMillis()
        
        repeat(100) {
            equalizer.processBuffer(inputBuffer, outputBuffer)
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        assertTrue("Processing should be fast enough for real-time", duration < 100)
    }
    
    @Test
    fun testPresetApplicationPerformance() {
        val equalizer = ParametricEqualizer(44100f, 10)
        
        val startTime = System.currentTimeMillis()
        
        repeat(1000) {
            equalizer.applyPreset(ParametricEqualizer.PRESET_BASS_BOOST)
            equalizer.applyPreset(ParametricEqualizer.PRESET_TREBLE_BOOST)
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        assertTrue("Preset application should be fast", duration < 500)
    }
}
