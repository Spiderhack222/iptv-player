package com.example.iptvplayer.data

import android.content.Context

class RecentChannelsManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun push(streamUrl: String) {
        val current = getRecent().toMutableList()
        current.remove(streamUrl)
        current.add(0, streamUrl)
        val trimmed = current.take(MAX_ITEMS)
        prefs.edit().putStringSet(KEY_RECENT, trimmed.toSet()).apply()
        prefs.edit().putString(KEY_RECENT_ORDER, trimmed.joinToString(SEPARATOR)).apply()
    }

    fun getRecent(): List<String> {
        val ordered = prefs.getString(KEY_RECENT_ORDER, null)
        if (!ordered.isNullOrBlank()) {
            return ordered.split(SEPARATOR).filter { it.isNotBlank() }
        }
        return prefs.getStringSet(KEY_RECENT, emptySet())?.toList() ?: emptyList()
    }

    companion object {
        private const val PREFS_NAME = "recent_channels_prefs"
        private const val KEY_RECENT = "recent_urls"
        private const val KEY_RECENT_ORDER = "recent_urls_order"
        private const val SEPARATOR = "||"
        private const val MAX_ITEMS = 80
    }
}
