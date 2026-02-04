package com.fumes.camerpc.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fumes.camerpc.data.CameraOption
import com.fumes.camerpc.data.CameraRepository
import com.fumes.camerpc.data.CameraRepositoryImpl
import com.fumes.camerpc.data.NetworkRepository
import com.fumes.camerpc.data.NetworkRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.fumes.camerpc.data.StreamingServer
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MainViewModel(
    private val networkRepository: NetworkRepository,
    private val cameraRepository: CameraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _forceKeyFrameEvent = MutableSharedFlow<Unit>()
    val forceKeyFrameEvent: SharedFlow<Unit> = _forceKeyFrameEvent.asSharedFlow()
    
    private val streamingServer = StreamingServer(port = 5000) {
        viewModelScope.launch {
            _forceKeyFrameEvent.emit(Unit)
        }
    }

    init {
        fetchIpAddress()
    }

    fun sendEncodedData(data: ByteArray, isKeyFrame: Boolean) {
        streamingServer.sendData(data, isKeyFrame)
    }

    fun loadCameras() {
        viewModelScope.launch {
            val cameras = cameraRepository.getAvailableCameras()
            _uiState.update { it.copy(
                availableCameras = cameras,
                selectedCamera = cameras.firstOrNull { c -> c.title.contains("1.0x") } ?: cameras.firstOrNull()
            ) }
        }
    }

    fun selectCamera(camera: CameraOption) {
        _uiState.update { it.copy(selectedCamera = camera) }
    }

    private fun fetchIpAddress() {
        viewModelScope.launch {
            val ip = networkRepository.getDeviceIpAddress()
            _uiState.update { it.copy(ipAddress = ip) }
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = isGranted) }
        if (isGranted) {
            loadCameras()
        }
    }

    fun toggleStreaming() {
        val newStreamingState = !_uiState.value.isStreaming
        _uiState.update { it.copy(isStreaming = newStreamingState) }
        
        if (newStreamingState) {
            viewModelScope.launch {
                streamingServer.start()
            }
        } else {
            streamingServer.stop()
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    NetworkRepositoryImpl(),
                    CameraRepositoryImpl(context)
                ) as T
            }
        }
    }
}
