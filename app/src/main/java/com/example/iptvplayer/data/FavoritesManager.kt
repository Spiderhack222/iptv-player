package com.example.iptvplayer.data

import android.content.Context

/**
 * Guarda los canales marcados como favoritos usando la URL del stream
 * como identificador único.
 */
class FavoritesManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isFavorite(streamUrl: String): Boolean =
        prefs.getStringSet(KEY_FAVORITES, emptySet())?.contains(streamUrl) == true

    fun toggleFavorite(streamUrl: String) {
        val current = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (!current.add(streamUrl)) {
            current.remove(streamUrl)
        }
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    fun getFavorites(): Set<String> =
        prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()

    companion object {
        private const val PREFS_NAME = "favorites_prefs"
        private const val KEY_FAVORITES = "favorite_urls"
    }
}
