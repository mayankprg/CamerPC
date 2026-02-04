package com.fumes.camerpc.data

import java.net.Inet4Address
import java.net.NetworkInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface NetworkRepository {
    suspend fun getDeviceIpAddress(): String
}

class NetworkRepositoryImpl : NetworkRepository {
    override suspend fun getDeviceIpAddress(): String = withContext(Dispatchers.IO) {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            
            // Priority 1: Check for wlan0 (standard Wi-Fi interface)
            val wifiInterface = interfaces.find { it.name.contains("wlan", ignoreCase = true) }
            wifiInterface?.inetAddresses?.toList()?.find { 
                !it.isLoopbackAddress && it is Inet4Address 
            }?.hostAddress?.let { return@withContext it }

            // Priority 2: Fallback to any non-loopback IPv4 (e.g. eth0, tethering)
            for (networkInterface in interfaces) {
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return@withContext address.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext "Unavailable"
    }
}
