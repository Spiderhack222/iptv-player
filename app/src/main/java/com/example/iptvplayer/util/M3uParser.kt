package com.example.iptvplayer.util

import com.example.iptvplayer.model.Channel

/**
 * Parser sencillo y robusto para listas de reproducción M3U / M3U8 con
 * extensiones habituales de IPTV (tvg-logo, group-title, etc.).
 *
 * Formato esperado:
 * #EXTM3U
 * #EXTINF:-1 tvg-logo="http://logo.png" group-title="Deportes",Canal Ejemplo
 * http://servidor/stream.m3u8
 */
object M3uParser {

    private val logoRegex = Regex("tvg-logo=\"([^\"]*)\"", RegexOption.IGNORE_CASE)
    private val groupRegex = Regex("group-title=\"([^\"]*)\"", RegexOption.IGNORE_CASE)
    private val nameRegex = Regex(",(.*)$")

    fun parse(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()

        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String? = null

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pendingLogo = logoRegex.find(line)?.groupValues?.get(1)
                    pendingGroup = groupRegex.find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                    pendingName = nameRegex.find(line)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
                }
                line.startsWith("#") -> {
                    // Otras directivas (#EXTM3U, #EXTGRP, #EXTVLCOPT, etc.) se ignoran.
                }
                else -> {
                    // Línea de URL de stream: cierra el canal pendiente.
                    val name = pendingName ?: line.substringAfterLast("/").ifBlank {
                        "Canal ${channels.size + 1}"
                    }
                    channels.add(
                        Channel(
                            name = name,
                            logoUrl = pendingLogo,
                            groupTitle = pendingGroup ?: "Sin categoría",
                            streamUrl = line
                        )
                    )
                    pendingName = null
                    pendingLogo = null
                    pendingGroup = null
                }
            }
        }
        return channels
    }
}
