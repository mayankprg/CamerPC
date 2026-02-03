package com.fumes.camerpc.data

import androidx.camera.core.CameraSelector

data class CameraOption(
    val id: String,
    val title: String,
    val selector: CameraSelector,
    val zoomRatio: Float? = null
)
