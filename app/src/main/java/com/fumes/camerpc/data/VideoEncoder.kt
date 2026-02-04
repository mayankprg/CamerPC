package com.fumes.camerpc.data

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer

class VideoEncoder {
    private var codec: MediaCodec? = null
    private var isRunning = false
    private var configData: ByteArray? = null
    private var dataCallback: ((ByteArray, Boolean) -> Unit)? = null

    /**
     * Starts the encoder and returns the Input Surface.
     * The caller must attach this Surface to the Camera.
     */
    fun start(width: Int, height: Int, callback: (ByteArray, Boolean) -> Unit): Surface? {
        if (isRunning) {
            Log.w("VideoEncoder", "Encoder already running")
            return null
        }

        this.dataCallback = callback
        configData = null

        try {
            val format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                width, height)
            format.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 2000000) // 2 Mbps
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 second between keyframes

            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            
            // Set Callback (Async Mode)
            codec?.setCallback(object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                    // Not used in Surface mode (Surface provides input)
                }

                override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                    if (!isRunning) return
                    try {
                        val outputBuffer = codec.getOutputBuffer(index)
                        if (outputBuffer == null) {
                            codec.releaseOutputBuffer(index, false)
                            return
                        }

                        // Handle Config Data (SPS/PPS)
                        if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {

                            configData = ByteArray(info.size)
                            outputBuffer.position(info.offset)
                            outputBuffer.limit(info.offset + info.size)
                            outputBuffer.get(configData!!)// Rewind just in case, though usually consumed
                            // We don't send config packet alone if we prepend it
                            codec.releaseOutputBuffer(index, false)
                            return
                        }

                        if (info.size > 0) {
                            val outData = ByteArray(info.size)
                            outputBuffer.get(outData)

                            val isKeyFrame = (info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0

                            // Prepend Config Data to Keyframes
                            if (isKeyFrame && configData != null) {
                                val combinedData = ByteArray(configData!!.size + outData.size)
                                System.arraycopy(configData!!, 0, combinedData, 0, configData!!.size)
                                System.arraycopy(outData, 0, combinedData, configData!!.size, outData.size)
                                dataCallback?.invoke(combinedData, true)
                            } else {
                                dataCallback?.invoke(outData, isKeyFrame)
                            }
                        }
                        codec.releaseOutputBuffer(index, false)
                    } catch (e: Exception) {
                        Log.e("VideoEncoder", "Error processing output buffer", e)
                    }
                }

                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                    Log.e("VideoEncoder", "Encoder error", e)
                    stop()
                }

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                    Log.d("VideoEncoder", "Output format changed: $format")
                }
            })

            codec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = codec?.createInputSurface()
            codec?.start()

            isRunning = true
            Log.d("VideoEncoder", "Encoder started with Surface: ${width}x${height}")
            return inputSurface

        } catch (e: Exception) {
            Log.e("VideoEncoder", "Failed to start encoder", e)
            isRunning = false
            return null
        }
    }

    fun stop() {
        if (!isRunning) return
        try {
            // Signal end of stream? With Surface, usually we just stop.
            codec?.stop()
            codec?.release()
        } catch (e: Exception) {
            Log.e("VideoEncoder", "Failed to stop encoder", e)
        } finally {
            codec = null
            isRunning = false
            configData = null
            dataCallback = null
            Log.d("VideoEncoder", "Encoder stopped")
        }
    }

    fun forceKeyFrame() {
        if (!isRunning) return
        try {
            val bundle = Bundle()
            bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
            codec?.setParameters(bundle)
            Log.d("VideoEncoder", "Forced KeyFrame requested")
        } catch (e: Exception) {
            Log.e("VideoEncoder", "Failed to force KeyFrame", e)
        }
    }
}
