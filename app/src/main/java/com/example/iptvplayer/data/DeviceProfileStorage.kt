package com.example.iptvplayer.data

import android.content.Context
import com.example.iptvplayer.model.DeviceProfile
import com.google.gson.Gson

class DeviceProfileStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getProfile(): DeviceProfile? {
        val json = prefs.getString(KEY_PROFILE, null) ?: return null
        return try {
            gson.fromJson(json, DeviceProfile::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun saveProfile(profile: DeviceProfile) {
        prefs.edit().putString(KEY_PROFILE, gson.toJson(profile)).apply()
    }

    fun clearProfile() {
        prefs.edit().remove(KEY_PROFILE).apply()
    }

    companion object {
        private const val PREFS_NAME = "device_profile_prefs"
        private const val KEY_PROFILE = "device_profile"
    }
}
