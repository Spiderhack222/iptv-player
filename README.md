# IPTV Player (M3U) — Android

App nativa en **Kotlin** que reproduce listas de canales **M3U / M3U8**, con una
experiencia similar a IBO Pro: pantalla de inicio para cargar listas, catálogo
de canales por categorías con buscador y favoritos, y reproductor a pantalla
completa basado en **ExoPlayer (Media3)**.

## Funcionalidades incluidas

- Cargar una lista M3U desde una **URL** o desde un **archivo local** (selector de archivos).
- Guardar listas cargadas por URL para volver a abrirlas después (persistente).
- Parser M3U propio que soporta `#EXTINF`, `tvg-logo`, `group-title`.
- Catálogo de canales agrupado por categoría (spinner), con **buscador** en vivo.
- Marcar/quitar canales como **favoritos** y filtrar solo favoritos.
- Reproductor a pantalla completa (orientación horizontal automática) con:
  - Soporte HLS (`.m3u8`) y otros formatos compatibles con ExoPlayer.
  - Controles nativos de reproducción (play/pausa, barra de progreso para streams que la soporten).
  - Botones para pasar al **canal siguiente/anterior** sin salir del reproductor.
  - Manejo de errores de reproducción (canal caído, formato no soportado, etc.).

## Qué NO incluye (posibles mejoras futuras)

- Login con **Xtream Codes API** (usuario/clave/servidor) — actualmente solo listas M3U/M3U8 planas.
- Guía electrónica de programación (EPG / XMLTV).
- Grabación de streams o reproducción en segundo plano (PiP).
- Perfiles de usuario / multi-perfil.

## Obtener el APK sin instalar nada (GitHub Actions)

Este proyecto incluye `.github/workflows/build-apk.yml`, que compila el APK
automáticamente en la nube. Pasos:

1. Crea un repositorio nuevo y **vacío** en GitHub (no le pongas README para
   evitar conflictos).
2. Sube esta carpeta al repositorio:
   ```bash
   cd IPTVPlayer
   git init
   git add .
   git commit -m "Proyecto inicial IPTV Player"
   git branch -M main
   git remote add origin https://github.com/TU_USUARIO/TU_REPO.git
   git push -u origin main
   ```
3. En GitHub, entra a la pestaña **Actions** de tu repositorio. Verás el
   workflow "Build APK" ejecutándose automáticamente (tarda 2–4 minutos).
4. Cuando termine (ícono verde ✅), entra a esa ejecución y baja hasta
   **Artifacts** → descarga `IPTVPlayer-debug-apk` (es un `.zip` que contiene
   el `app-debug.apk`).
5. Copia el APK a tu teléfono e instálalo (activa "Instalar apps de fuentes
   desconocidas" si Android lo pide). Es un APK de depuración (debug), así
   que no necesita firma para instalarse manualmente.

Si prefieres no usar Git por línea de comandos, también puedes crear el
repositorio y subir el ZIP descomprimido directamente desde la interfaz web
de GitHub (botón "Add file → Upload files").

## Cómo abrir y compilar el proyecto (con Android Studio)

1. Instala **Android Studio** (versión Hedgehog/2023.1 o más reciente).
2. Abre Android Studio → **Open** → selecciona la carpeta `IPTVPlayer` (esta carpeta).
3. Deja que Gradle sincronice (descargará automáticamente las dependencias:
   AndroidX, Material Components, Glide, Gson, Media3/ExoPlayer, Coroutines).
4. Conecta un dispositivo Android (o usa un emulador) y pulsa **Run ▶**.
5. Para generar el APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

Requisitos: `minSdk 21` (Android 5.0+), `targetSdk/compileSdk 34`.

## Estructura del proyecto

```
IPTVPlayer/
├── app/
│   ├── build.gradle                 # Dependencias del módulo
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/iptvplayer/
│       │   ├── MainActivity.kt      # Pantalla de carga de listas
│       │   ├── ChannelsActivity.kt  # Catálogo de canales
│       │   ├── PlayerActivity.kt    # Reproductor ExoPlayer
│       │   ├── model/               # Channel, Playlist
│       │   ├── util/                # M3uParser, PlaylistFetcher
│       │   ├── data/                # Repositorio, almacenamiento, favoritos
│       │   └── adapter/             # Adapters de RecyclerView
│       └── res/                     # Layouts, iconos, colores, strings
├── build.gradle
└── settings.gradle
```

## Cómo probar rápido

Puedes probar con cualquier URL pública de lista M3U/M3U8 de prueba (por
ejemplo, listas gratuitas de canales de noticias/documentales de libre
distribución) o con tu propia lista IPTV, pegándola en el campo de texto de
la pantalla principal.

## Notas técnicas

- El manifiesto habilita `usesCleartextTraffic` y una configuración de
  seguridad de red permisiva porque muchas listas IPTV usan enlaces `http://`
  sin cifrar; si tu lista es solo `https://`, puedes endurecer esta
  configuración.
- El parser de M3U es tolerante a formatos ligeramente distintos entre
  proveedores (algunos omiten `group-title` o `tvg-logo`); en esos casos el
  canal se asigna a "Sin categoría" y no muestra logo.
- Los favoritos y las listas guardadas se almacenan localmente en el
  dispositivo (SharedPreferences); no se sincronizan en la nube.
