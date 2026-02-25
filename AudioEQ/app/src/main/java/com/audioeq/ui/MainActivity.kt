package com.audioeq.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.audioeq.R
import com.audioeq.databinding.ActivityMainBinding
import com.audioeq.equalizer.EqualizerPreset
import com.audioeq.equalizer.ParametricEqualizer
import com.audioeq.service.AudioProcessingService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    
    private var audioService: AudioProcessingService? = null
    private var isBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioProcessingService.LocalBinder
            audioService = binder.getService()
            isBound = true
            
            audioService?.onProcessingStateChanged = { state ->
                lifecycleScope.launch {
                    viewModel.setProcessingState(state == com.audioeq.service.ProcessingState.RUNNING)
                }
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            isBound = false
        }
    }
    
    private val mediaProjectionManager by lazy {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    
    private val requestMediaProjection = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            startAudioProcessing(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Media projection permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            requestMediaProjection()
        } else {
            Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        binding.btnStartStop.setOnClickListener {
            if (viewModel.isProcessing.value) {
                stopAudioProcessing()
            } else {
                checkPermissionsAndStart()
            }
        }
        
        val presetNames = ParametricEqualizer.ALL_PRESETS.map { it.name }.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, presetNames)
        binding.spinnerPreset.setAdapter(adapter)
        
        // Make it behave like a dropdown
        binding.spinnerPreset.setOnClickListener {
            binding.spinnerPreset.showDropDown()
        }
        
        binding.spinnerPreset.setOnItemClickListener { _, _, position, _ ->
            val preset = ParametricEqualizer.ALL_PRESETS[position]
            audioService?.setEqualizerPreset(preset)
            viewModel.selectPreset(preset)
        }
        
        binding.equalizerView.onBandChanged = { bandIndex, gain ->
            audioService?.setBandGain(bandIndex, gain)
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isProcessing.collect { isProcessing ->
                updateProcessingUI(isProcessing)
            }
        }
        
        lifecycleScope.launch {
            viewModel.selectedPreset.collect { preset ->
                preset?.let {
                    binding.equalizerView.setBands(it.bands)
                }
            }
        }
    }
    
    private fun updateProcessingUI(isProcessing: Boolean) {
        binding.btnStartStop.text = if (isProcessing) "Stop" else "Start"
        binding.btnStartStop.setBackgroundColor(
            ContextCompat.getColor(
                this,
                if (isProcessing) android.R.color.holo_red_dark else android.R.color.holo_green_dark
            )
        )
        binding.statusText.text = if (isProcessing) "Processing audio..." else "Ready"
    }
    
    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        permissions.add(Manifest.permission.RECORD_AUDIO)
        
        val needsRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (needsRequest) {
            requestPermissions.launch(permissions.toTypedArray())
        } else {
            requestMediaProjection()
        }
    }
    
    private fun requestMediaProjection() {
        showMediaProjectionExplanation()
    }
    
    private fun showMediaProjectionExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Audio Capture Permission")
            .setMessage("This app needs permission to capture audio from other apps. Please grant the permission in the next screen.")
            .setPositiveButton("Continue") { _, _ ->
                requestMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun startAudioProcessing(resultCode: Int, data: Intent) {
        val serviceIntent = AudioProcessingService.createStartIntent(this, resultCode, data)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun stopAudioProcessing() {
        audioService?.stopProcessing()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
    
    override fun onStart() {
        super.onStart()
        if (!isBound) {
            val intent = Intent(this, AudioProcessingService::class.java)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    
    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}
