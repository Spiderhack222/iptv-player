package com.example.iptvplayer.model

import java.io.Serializable

/**
 * Representa un canal individual extraído de una lista M3U.
 */
data class Channel(
    val name: String,
    val logoUrl: String?,
    val groupTitle: String?,
    val streamUrl: String
) : Serializable
