package com.example.iptvplayer.model

enum class ContentModule(val id: String, val displayName: String) {
    LIVE_TV("live", "Live TV"),
    MOVIES("movies", "Movies"),
    SERIES("series", "Series"),
    RADIOS("radios", "Radios");

    companion object {
        fun fromId(id: String?): ContentModule =
            entries.firstOrNull { it.id == id } ?: LIVE_TV

        fun inferFrom(channel: Channel): ContentModule {
            val fingerprint = listOf(channel.name, channel.groupTitle, channel.streamUrl)
                .joinToString(" ")
                .lowercase()

            return when {
                fingerprint.contains("radio") || fingerprint.contains("fm") ||
                    fingerprint.contains("music") -> RADIOS

                fingerprint.contains("series") || fingerprint.contains("season") ||
                    fingerprint.contains("episod") || fingerprint.contains("tv show") -> SERIES

                fingerprint.contains("movie") || fingerprint.contains("film") ||
                    fingerprint.contains("pelicul") || fingerprint.contains("vod") ||
                    fingerprint.contains("cinema") -> MOVIES

                else -> LIVE_TV
            }
        }
    }
}
