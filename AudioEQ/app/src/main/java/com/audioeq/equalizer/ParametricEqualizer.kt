package com.audioeq.equalizer

import com.audioeq.equalizer.FilterType.*

data class EqualizerPreset(
    val name: String,
    val bands: List<FilterBand>
)

class ParametricEqualizer(
    private val sampleRate: Float,
    private val numBands: Int = 10
) {
    private var bands: MutableList<FilterBand> = mutableListOf()
    private var filters: MutableList<BiquadFilter> = mutableListOf()
    
    var isEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                filters.forEach { it.reset() }
            }
        }
    
    init {
        initializeDefaultBands()
        rebuildFilters()
    }
    
    private fun initializeDefaultBands() {
        val defaultFrequencies = listOf(
            32f, 64f, 125f, 250f, 500f, 
            1000f, 2000f, 4000f, 8000f, 16000f
        )
        
        bands.clear()
        defaultFrequencies.take(numBands).forEach { freq ->
            bands.add(
                FilterBand(
                    frequency = freq,
                    gain = 0f,
                    q = 1.0f,
                    type = when {
                        freq <= 64f -> LOW_SHELF
                        freq >= 8000f -> HIGH_SHELF
                        else -> PEAKING
                    }
                )
            )
        }
    }
    
    private fun rebuildFilters() {
        filters.clear()
        bands.forEach { band ->
            filters.add(BiquadFilter(sampleRate, band))
        }
    }
    
    fun setBandGain(bandIndex: Int, gain: Float) {
        if (bandIndex in bands.indices) {
            val oldBand = bands[bandIndex]
            bands[bandIndex] = oldBand.copy(gain = gain)
            filters[bandIndex] = BiquadFilter(sampleRate, bands[bandIndex])
        }
    }
    
    fun setBandFrequency(bandIndex: Int, frequency: Float) {
        if (bandIndex in bands.indices) {
            val oldBand = bands[bandIndex]
            bands[bandIndex] = oldBand.copy(frequency = frequency)
            filters[bandIndex] = BiquadFilter(sampleRate, bands[bandIndex])
        }
    }
    
    fun setBandQ(bandIndex: Int, q: Float) {
        if (bandIndex in bands.indices) {
            val oldBand = bands[bandIndex]
            bands[bandIndex] = oldBand.copy(q = q)
            filters[bandIndex] = BiquadFilter(sampleRate, bands[bandIndex])
        }
    }
    
    fun setBandType(bandIndex: Int, type: FilterType) {
        if (bandIndex in bands.indices) {
            val oldBand = bands[bandIndex]
            bands[bandIndex] = oldBand.copy(type = type)
            filters[bandIndex] = BiquadFilter(sampleRate, bands[bandIndex])
        }
    }
    
    fun applyPreset(preset: EqualizerPreset) {
        bands.clear()
        bands.addAll(preset.bands)
        rebuildFilters()
    }
    
    fun getBands(): List<FilterBand> = bands.toList()
    
    fun process(sample: Double): Double {
        if (!isEnabled) return sample
        
        var output = sample
        filters.forEach { filter ->
            output = filter.process(output)
        }
        return output
    }
    
    fun processBuffer(buffer: ShortArray, outputBuffer: ShortArray) {
        for (i in buffer.indices) {
            val sample = buffer[i].toDouble() / Short.MAX_VALUE
            val processed = process(sample)
            outputBuffer[i] = (processed * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }
    
    fun reset() {
        filters.forEach { it.reset() }
    }
    
    companion object {
        val PRESET_FLAT = EqualizerPreset(
            name = "Flat",
            bands = listOf(
                FilterBand(32f, 0f, 1.0f, LOW_SHELF),
                FilterBand(64f, 0f, 1.0f, PEAKING),
                FilterBand(125f, 0f, 1.0f, PEAKING),
                FilterBand(250f, 0f, 1.0f, PEAKING),
                FilterBand(500f, 0f, 1.0f, PEAKING),
                FilterBand(1000f, 0f, 1.0f, PEAKING),
                FilterBand(2000f, 0f, 1.0f, PEAKING),
                FilterBand(4000f, 0f, 1.0f, PEAKING),
                FilterBand(8000f, 0f, 1.0f, PEAKING),
                FilterBand(16000f, 0f, 1.0f, HIGH_SHELF)
            )
        )
        
        val PRESET_BASS_BOOST = EqualizerPreset(
            name = "Bass Boost",
            bands = listOf(
                FilterBand(32f, 6f, 1.0f, LOW_SHELF),
                FilterBand(64f, 5f, 1.0f, PEAKING),
                FilterBand(125f, 3f, 1.0f, PEAKING),
                FilterBand(250f, 0f, 1.0f, PEAKING),
                FilterBand(500f, 0f, 1.0f, PEAKING),
                FilterBand(1000f, 0f, 1.0f, PEAKING),
                FilterBand(2000f, 0f, 1.0f, PEAKING),
                FilterBand(4000f, 0f, 1.0f, PEAKING),
                FilterBand(8000f, 0f, 1.0f, PEAKING),
                FilterBand(16000f, 0f, 1.0f, HIGH_SHELF)
            )
        )
        
        val PRESET_TREBLE_BOOST = EqualizerPreset(
            name = "Treble Boost",
            bands = listOf(
                FilterBand(32f, 0f, 1.0f, LOW_SHELF),
                FilterBand(64f, 0f, 1.0f, PEAKING),
                FilterBand(125f, 0f, 1.0f, PEAKING),
                FilterBand(250f, 0f, 1.0f, PEAKING),
                FilterBand(500f, 0f, 1.0f, PEAKING),
                FilterBand(1000f, 0f, 1.0f, PEAKING),
                FilterBand(2000f, 2f, 1.0f, PEAKING),
                FilterBand(4000f, 4f, 1.0f, PEAKING),
                FilterBand(8000f, 5f, 1.0f, PEAKING),
                FilterBand(16000f, 6f, 1.0f, HIGH_SHELF)
            )
        )
        
        val PRESET_VOCAL = EqualizerPreset(
            name = "Vocal",
            bands = listOf(
                FilterBand(32f, -2f, 1.0f, LOW_SHELF),
                FilterBand(64f, -1f, 1.0f, PEAKING),
                FilterBand(125f, 0f, 1.0f, PEAKING),
                FilterBand(250f, 2f, 1.0f, PEAKING),
                FilterBand(500f, 4f, 1.0f, PEAKING),
                FilterBand(1000f, 4f, 1.0f, PEAKING),
                FilterBand(2000f, 3f, 1.0f, PEAKING),
                FilterBand(4000f, 2f, 1.0f, PEAKING),
                FilterBand(8000f, 0f, 1.0f, PEAKING),
                FilterBand(16000f, -1f, 1.0f, HIGH_SHELF)
            )
        )
        
        val ALL_PRESETS = listOf(
            PRESET_FLAT,
            PRESET_BASS_BOOST,
            PRESET_TREBLE_BOOST,
            PRESET_VOCAL
        )
    }
}
