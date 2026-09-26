package com.example.iptvplayer.data

import com.example.iptvplayer.model.Channel

/**
 * Mantiene en memoria la lista de canales cargada actualmente para que
 * pueda compartirse fácilmente entre ChannelsActivity y PlayerActivity
 * (evita pasar listas grandes por Intent).
 */
object PlaylistRepository {
    var channels: List<Channel> = emptyList()
    var currentPlaylistName: String = ""
}
