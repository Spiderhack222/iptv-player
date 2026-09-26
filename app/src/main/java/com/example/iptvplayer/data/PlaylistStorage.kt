package com.example.iptvplayer.data

import android.content.Context
import com.example.iptvplayer.model.Playlist
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Guarda de forma persistente las listas M3U cargadas por URL para que
 * el usuario pueda volver a abrirlas sin escribir la URL de nuevo.
 */
class PlaylistStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getPlaylists(): MutableList<Playlist> {
        val json = prefs.getString(KEY_PLAYLISTS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Playlist>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun addPlaylist(playlist: Playlist) {
        val list = getPlaylists()
        list.removeAll { it.source == playlist.source }
        list.add(0, playlist)
        save(list)
    }

    fun removePlaylist(playlist: Playlist) {
        val list = getPlaylists()
        list.removeAll { it.id == playlist.id }
        save(list)
    }

    private fun save(list: MutableList<Playlist>) {
        prefs.edit().putString(KEY_PLAYLISTS, gson.toJson(list)).apply()
    }

    companion object {
        private const val PREFS_NAME = "playlists_prefs"
        private const val KEY_PLAYLISTS = "saved_playlists"
    }
}
