package com.fumes.camerpc.ui.main

data class MainUiState(
    val isStreaming: Boolean = false,
    val ipAddress: String = "Loading...",
    val hasCameraPermission: Boolean = false
)
