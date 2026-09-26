package com.example.iptvplayer.data

import com.example.iptvplayer.model.Channel

object PlaylistRepository {
    var channels: List<Channel> = emptyList()
    var currentPlaylistName: String = ""
    var currentPlaylistSource: String = ""
}
