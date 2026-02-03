package com.fumes.camerpc.ui.main

import com.fumes.camerpc.data.CameraOption

data class MainUiState(
    val isStreaming: Boolean = false,
    val ipAddress: String = "Fetching...",
    val hasCameraPermission: Boolean = false,
    val availableCameras: List<CameraOption> = emptyList(),
    val selectedCamera: CameraOption? = null
)
