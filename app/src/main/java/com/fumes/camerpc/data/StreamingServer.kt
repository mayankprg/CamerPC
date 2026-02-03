package com.fumes.camerpc.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class StreamingServer(private val port: Int = 5000) {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputStream: OutputStream? = null
    private val isRunning = AtomicBoolean(false)

    suspend fun start() = withContext(Dispatchers.IO) {
        if (isRunning.get()) return@withContext
        
        try {
            serverSocket = ServerSocket(port)
            isRunning.set(true)
            Log.d("StreamingServer", "Server started on port $port. Waiting for connection...")
            
            // Wait for a single client (the PC) to connect
            clientSocket = serverSocket?.accept()
            outputStream = clientSocket?.getOutputStream()
            Log.d("StreamingServer", "Client connected: ${clientSocket?.inetAddress}")
        } catch (e: Exception) {
            Log.e("StreamingServer", "Error starting server", e)
            stop()
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            outputStream?.close()
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e("StreamingServer", "Error stopping server", e)
        } finally {
            outputStream = null
            clientSocket = null
            serverSocket = null
            Log.d("StreamingServer", "Server stopped")
        }
    }

    fun sendData(data: ByteArray) {
        if (!isRunning.get() || outputStream == null) return
        
        try {
            outputStream?.write(data)
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e("StreamingServer", "Error sending data, client might have disconnected", e)
            // If connection fails, we don't stop the whole server, but we could handle reconnection logic
        }
    }
    
    fun isClientConnected(): Boolean = clientSocket?.isConnected == true && !clientSocket!!.isClosed
}
