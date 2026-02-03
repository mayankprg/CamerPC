package com.fumes.camerpc.data

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class VideoEncoder {
    private var codec: MediaCodec? = null
    private var isRunning = false
    private var startTimeUs: Long = 0

    fun start(width: Int, height: Int) {
        if (isRunning) return
        
        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 2000000) // 2 Mbps
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec?.start()
            
            isRunning = true
            startTimeUs = System.currentTimeMillis() * 1000
            Log.d("VideoEncoder", "Encoder started: ${width}x${height}")
        } catch (e: Exception) {
            Log.e("VideoEncoder", "Failed to start encoder", e)
            isRunning = false
        }
    }

    fun stop() {
        if (!isRunning) return
        try {
            codec?.stop()
            codec?.release()
            codec = null
            isRunning = false
            Log.d("VideoEncoder", "Encoder stopped")
        } catch (e: Exception) {
            Log.e("VideoEncoder", "Failed to stop encoder", e)
        }
    }

    fun encode(image: ImageProxy): ByteArray? {
        if (!isRunning || codec == null) {
            image.close()
            return null
        }

        try {
            // 1. Convert ImageProxy to NV12
            val nv12Data = YuvUtils.yuv420888ToNv12(image)
            image.close() // Close the image as soon as we copied the data

            // 2. Input to MediaCodec
            val inputBufferIndex = codec!!.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                val inputBuffer = codec!!.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                inputBuffer?.put(nv12Data)
                
                val presentationTimeUs = System.currentTimeMillis() * 1000 - startTimeUs
                codec!!.queueInputBuffer(inputBufferIndex, 0, nv12Data.size, presentationTimeUs, 0)
            }

            // 3. Output from MediaCodec (Drain)
            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = codec!!.dequeueOutputBuffer(bufferInfo, 0)
            
            // For simplicity in this step, we just grab the first available output.
            // In a real stream, we might loop until tryAgainLater.
            if (outputBufferIndex >= 0) {
                val outputBuffer = codec!!.getOutputBuffer(outputBufferIndex)
                val outData = ByteArray(bufferInfo.size)
                outputBuffer?.get(outData)
                
                codec!!.releaseOutputBuffer(outputBufferIndex, false)
                return outData
            }
        } catch (e: Exception) {
            Log.e("VideoEncoder", "Encoding failed", e)
        }
        return null
    }
}
