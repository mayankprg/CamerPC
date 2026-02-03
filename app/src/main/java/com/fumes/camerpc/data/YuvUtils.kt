package com.fumes.camerpc.data

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

object YuvUtils {
    fun imageToNv21(image: ImageProxy): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }
    
    // Helper to populate a ByteBuffer from ImageProxy for MediaCodec
    // Converts YUV_420_888 to NV12 (YUV420SemiPlanar) which is standard for MediaCodec
    fun yuv420888ToNv12(image: ImageProxy): ByteArray {
        val width = image.width
        val height = image.height
        
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val numPixels = (width * height)
        val nv12 = ByteArray(numPixels * 3 / 2)
        
        // Copy Y
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride // usually 1
        
        // If packed correctly, we can bulk copy. If not (due to stride), row by row.
        if (yRowStride == width) {
            yBuffer.get(nv12, 0, numPixels)
        } else {
            var outputPos = 0
            for (row in 0 until height) {
                yBuffer.position(row * yRowStride)
                yBuffer.get(nv12, outputPos, width)
                outputPos += width
            }
        }

        // Copy UV (Interleaved)
        // NV12 is Y followed by UV interleaved.
        // CameraX YUV_420_888 often has separate U and V planes, but they might be part of the same buffer differently offset.
        // For NV12, we want U and V interleaved: U V U V... 
        // Wait, NV12 is UVUV or VUVU? NV12 is Y followed by UV. NV21 is Y followed by VU.
        // Let's assume NV12 (standard for AVC encoders on Android)
        
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride
        
        val uvHeight = height / 2
        val uvWidth = width / 2
        
        var offset = numPixels
        
        for (row in 0 until uvHeight) {
            for (col in 0 until uvWidth) {
                val uIndex = row * uvRowStride + col * uvPixelStride
                val vIndex = row * vPlane.rowStride + col * vPlane.pixelStride
                
                nv12[offset++] = uBuffer.get(uIndex) // U
                nv12[offset++] = vBuffer.get(vIndex) // V
            }
        }
        
        return nv12
    }
}
