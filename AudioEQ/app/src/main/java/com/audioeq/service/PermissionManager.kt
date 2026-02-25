package com.audioeq.service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionState(
    val hasRecordAudio: Boolean = false,
    val hasPostNotifications: Boolean = false,
    val hasMediaProjection: Boolean = false,
    val allGranted: Boolean = false
)

class PermissionManager(private val context: Context) {
    
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    
    private val mediaProjectionManager by lazy {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    
    fun checkPermissions() {
        val hasRecordAudio = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasPostNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        _permissionState.value = _permissionState.value.copy(
            hasRecordAudio = hasRecordAudio,
            hasPostNotifications = hasPostNotifications,
            allGranted = hasRecordAudio && hasPostNotifications
        )
    }
    
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
    }
    
    fun createMediaProjectionIntent() = mediaProjectionManager.createScreenCaptureIntent()
    
    fun setMediaProjectionGranted() {
        _permissionState.value = _permissionState.value.copy(
            hasMediaProjection = true,
            allGranted = _permissionState.value.hasRecordAudio && 
                        _permissionState.value.hasPostNotifications
        )
    }
    
    fun reset() {
        _permissionState.value = PermissionState()
    }
}
