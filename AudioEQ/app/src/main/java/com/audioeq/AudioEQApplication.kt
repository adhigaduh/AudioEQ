package com.audioeq

import android.app.Application
import com.audioeq.equalizer.EqualizerPreset

class AudioEQApplication : Application() {
    
    companion object {
        lateinit var instance: AudioEQApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
