package com.audioeq.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audioeq.equalizer.EqualizerPreset
import com.audioeq.equalizer.ParametricEqualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _selectedPreset = MutableStateFlow<EqualizerPreset?>(null)
    val selectedPreset: StateFlow<EqualizerPreset?> = _selectedPreset.asStateFlow()
    
    private val _bands = MutableStateFlow(ParametricEqualizer.PRESET_FLAT.bands)
    val bands: StateFlow<List<com.audioeq.equalizer.FilterBand>> = _bands.asStateFlow()
    
    fun setProcessingState(isProcessing: Boolean) {
        viewModelScope.launch {
            _isProcessing.value = isProcessing
        }
    }
    
    fun selectPreset(preset: EqualizerPreset) {
        viewModelScope.launch {
            _selectedPreset.value = preset
            _bands.value = preset.bands
        }
    }
    
    fun updateBand(bandIndex: Int, gain: Float) {
        viewModelScope.launch {
            val currentBands = _bands.value.toMutableList()
            if (bandIndex in currentBands.indices) {
                currentBands[bandIndex] = currentBands[bandIndex].copy(gain = gain)
                _bands.value = currentBands
            }
        }
    }
}
