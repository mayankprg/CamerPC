package com.fumes.camerpc.data

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.CameraInfo
import androidx.camera.camera2.interop.Camera2CameraInfo
import android.hardware.camera2.CameraCharacteristics
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import com.fumes.camerpc.util.await

interface CameraRepository {
    suspend fun getAvailableCameras(): List<CameraOption>
}

class CameraRepositoryImpl(private val context: Context) : CameraRepository {
    @OptIn(ExperimentalCamera2Interop::class)
    override suspend fun getAvailableCameras(): List<CameraOption> {
        val cameraProvider = ProcessCameraProvider.getInstance(context).await()
        val availableCameras = mutableListOf<CameraOption>()
        
        // distinct by lens facing to avoid duplicates if multiple logical cameras exist
        // (though usually only one Back and one Front are exposed by CameraX by default)
        val cameraInfos = cameraProvider.availableCameraInfos
            .distinctBy { 
                try {
                    val c2 = Camera2CameraInfo.from(it)
                    c2.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                } catch (e: Exception) {
                    null
                }
            }

        for (cameraInfo in cameraInfos) {
            val selector = try {
                 // Try to create a selector for this specific camera
                 // Note: CameraSelector.Builder().addCameraFilter { ... } could be used to select THIS specific cameraInfo
                 // But simpler is to rely on LensFacing for standard Front/Back
                 val c2Info = Camera2CameraInfo.from(cameraInfo)
                 val level = c2Info.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                 
                 if (level == CameraCharacteristics.LENS_FACING_BACK) {
                     CameraSelector.DEFAULT_BACK_CAMERA
                 } else if (level == CameraCharacteristics.LENS_FACING_FRONT) {
                     CameraSelector.DEFAULT_FRONT_CAMERA
                 } else {
                     null
                 }
            } catch (e: Exception) {
                null
            }

            if (selector != null) {
                val c2Info = Camera2CameraInfo.from(cameraInfo)
                val facing = c2Info.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    // Add requested options for Rear camera
                    // We optimistically add them. In a real robust app, we should check capabilities.
                    // But checking max/min zoom requires binding or complex calculation from characteristics.
                    
                    availableCameras.add(
                        CameraOption(
                            id = "rear_0_6x",
                            title = "Rear 0.6x",
                            selector = selector,
                            zoomRatio = 0.6f
                        )
                    )
                    availableCameras.add(
                        CameraOption(
                            id = "rear_1_0x",
                            title = "Rear 1.0x",
                            selector = selector,
                            zoomRatio = 1.0f
                        )
                    )
                    availableCameras.add(
                        CameraOption(
                            id = "rear_3_0x",
                            title = "Rear 3.0x",
                            selector = selector,
                            zoomRatio = 3.0f
                        )
                    )
                } else if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    availableCameras.add(
                        CameraOption(
                            id = "front",
                            title = "Front",
                            selector = selector,
                            zoomRatio = 1.0f
                        )
                    )
                }
            }
        }
        
        return availableCameras
    }
}
