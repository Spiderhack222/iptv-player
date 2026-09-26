package com.example.iptvplayer.model

import java.util.UUID

/**
 * Representa una lista M3U guardada por el usuario (nombre + URL de origen).
 */
data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val source: String
)
