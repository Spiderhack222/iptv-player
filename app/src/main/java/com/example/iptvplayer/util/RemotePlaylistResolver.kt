package com.example.iptvplayer.util

import android.net.Uri
import com.example.iptvplayer.model.DeviceProfile

object RemotePlaylistResolver {

    fun buildUrl(profile: DeviceProfile): String {
        val base = profile.portalUrl.trim()
        if (base.contains("{mac}", ignoreCase = true)) {
            return base
                .replace("{mac}", profile.macAddress, ignoreCase = true)
                .replace("{code}", profile.accessCode, ignoreCase = true)
        }

        val uri = Uri.parse(base)
        val builder = uri.buildUpon()
        if (uri.getQueryParameter("mac").isNullOrBlank()) {
            builder.appendQueryParameter("mac", profile.macAddress)
        }
        if (profile.accessCode.isNotBlank() && uri.getQueryParameter("code").isNullOrBlank()) {
            builder.appendQueryParameter("code", profile.accessCode)
        }
        return builder.build().toString()
    }

    fun maskMac(mac: String): String {
        val clean = mac.trim()
        if (clean.length < 5) return clean
        return "${clean.take(8)}:**:**:${clean.takeLast(2)}"
    }
}
