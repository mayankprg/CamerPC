package com.fumes.camerpc.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

import java.net.InetSocketAddress

class StreamingServer(
    private val port: Int = 5000,
    private val onClientConnected: () -> Unit = {}
) {
    private var serverSocket: ServerSocket? = null
    @Volatile private var outputStream: OutputStream? = null
    private var clientSocket: Socket? = null
    private val isRunning = AtomicBoolean(false)
    private val isWaitingForKeyFrame = AtomicBoolean(true)
    
    private data class StreamData(val data: ByteArray, val isKeyFrame: Boolean)
    private var dataChannel = Channel<StreamData>(capacity = Channel.CONFLATED)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var senderJob: kotlinx.coroutines.Job? = null

    suspend fun start() = withContext(Dispatchers.IO) {
        if (isRunning.get()) return@withContext
        
        try {
            dataChannel = Channel(capacity = Channel.CONFLATED)
            serverSocket = ServerSocket()
            serverSocket?.reuseAddress = true
            serverSocket?.bind(InetSocketAddress("0.0.0.0", port))
            
            isRunning.set(true)
            Log.d("StreamingServer", "Server started on port $port. Waiting for connections...")
            
            // Start the sender loop
            senderJob = scope.launch {
                try {
                    for (streamData in dataChannel) {
                        if (isWaitingForKeyFrame.get() && !streamData.isKeyFrame) {
                            continue // Skip until we get a keyframe for the new client
                        }
                        isWaitingForKeyFrame.set(false)

                        val stream = outputStream
                        if (stream != null) {
                            try {
                                withContext(Dispatchers.IO) {
                                    stream.write(streamData.data)
                                }
                            } catch (e: Exception) {
                                Log.e("StreamingServer", "Error sending data, client disconnected", e)
                                cleanupClient()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StreamingServer", "Sender job cancelled or failed", e)
                }
            }
            
            while (isRunning.get()) {
                try {
                    // Accept a new connection
                    val socket = serverSocket?.accept()
                    if (socket != null) {
                        Log.d("StreamingServer", "Client connected: ${socket.inetAddress}")
                        
                        // Close previous connection if exists
                        cleanupClient()
                        
                        clientSocket = socket
                        outputStream = socket.getOutputStream()
                        isWaitingForKeyFrame.set(true)
                        onClientConnected()
                        
                        // We stay in this state until write fails or server stops
                        // The loop continues only if accept throws or we want to support multiple (which we don't here, strictly)
                        // Actually, accept() blocks. We need to be able to accept a NEW connection if the old one dies.
                        // But if we are blocked on accept(), we can't detect the old one died easily unless we write to it.
                        // So: We accept, set the stream, and go back to accept? 
                        // If we go back to accept, we block again. If the FIRST client is still alive, we are fine.
                        // If a SECOND client comes, we overwrite the first. This is acceptable for a simple streamer.
                    }
                } catch (e: Exception) {
                    if (isRunning.get()) {
                        Log.e("StreamingServer", "Error accepting connection", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StreamingServer", "Error starting server", e)
        } finally {
            stop()
        }
    }

    fun stop() {
        isRunning.set(false)
        dataChannel.close()
        senderJob?.cancel()
        cleanupClient()
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e("StreamingServer", "Error closing server socket", e)
        } finally {
            serverSocket = null
            Log.d("StreamingServer", "Server stopped")
        }
    }

    private fun cleanupClient() {
        try {
            outputStream?.close()
            clientSocket?.close()
        } catch (e: Exception) {
            // Ignore
        } finally {
            outputStream = null
            clientSocket = null
        }
    }

    fun sendData(data: ByteArray, isKeyFrame: Boolean) {
        if (!isRunning.get()) return
        dataChannel.trySend(StreamData(data, isKeyFrame))
    }
}
