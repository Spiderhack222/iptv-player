package com.example.iptvplayer.model

data class DeviceProfile(
    val portalUrl: String,
    val macAddress: String,
    val accessCode: String = "",
    val profileName: String = "MAC Remote",
    val autoReconnect: Boolean = true,
    val lastConnectedAt: Long = 0L
)
