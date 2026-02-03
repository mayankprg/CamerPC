package com.fumes.camerpc.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fumes.camerpc.data.NetworkRepository
import com.fumes.camerpc.data.NetworkRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val networkRepository: NetworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        fetchIpAddress()
    }

    private fun fetchIpAddress() {
        viewModelScope.launch {
            val ip = networkRepository.getDeviceIpAddress()
            _uiState.update { it.copy(ipAddress = ip) }
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = isGranted) }
    }

    fun toggleStreaming() {
        _uiState.update { it.copy(isStreaming = !it.isStreaming) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(NetworkRepositoryImpl()) as T
            }
        }
    }
}
