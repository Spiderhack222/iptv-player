package com.example.iptvplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.iptvplayer.adapter.PlaylistAdapter
import com.example.iptvplayer.data.PlaylistRepository
import com.example.iptvplayer.data.PlaylistStorage
import com.example.iptvplayer.databinding.ActivityMainBinding
import com.example.iptvplayer.model.Playlist
import com.example.iptvplayer.util.M3uParser
import com.example.iptvplayer.util.PlaylistFetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var storage: PlaylistStorage
    private lateinit var adapter: PlaylistAdapter

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { loadFromUri(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storage = PlaylistStorage(this)

        adapter = PlaylistAdapter(
            storage.getPlaylists(),
            onClick = { playlist -> loadFromUrl(playlist.source, playlist.name, save = false) },
            onDelete = { playlist ->
                storage.removePlaylist(playlist)
                adapter.updateList(storage.getPlaylists())
            }
        )
        binding.recyclerPlaylists.layoutManager = LinearLayoutManager(this)
        binding.recyclerPlaylists.adapter = adapter

        binding.btnLoadUrl.setOnClickListener {
            val url = binding.editUrl.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "Ingresa una URL de lista M3U", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            askNameAndLoad(url)
        }

        binding.btnLoadFile.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }
    }

    private fun askNameAndLoad(source: String) {
        val input = EditText(this)
        input.hint = "Nombre de la lista (opcional)"
        AlertDialog.Builder(this)
            .setTitle("Guardar lista")
            .setView(input)
            .setPositiveButton("Cargar") { _, _ ->
                val name = input.text.toString().ifBlank { "Lista sin nombre" }
                loadFromUrl(source, name, save = true)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun loadFromUrl(url: String, name: String, save: Boolean) {
        setLoading(true)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val content = PlaylistFetcher.fetchFromUrl(url)
                val channels = withContext(Dispatchers.Default) { M3uParser.parse(content) }
                if (channels.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "No se encontraron canales en la lista",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    if (save) {
                        storage.addPlaylist(Playlist(name = name, source = url))
                        adapter.updateList(storage.getPlaylists())
                    }
                    PlaylistRepository.channels = channels
                    PlaylistRepository.currentPlaylistName = name
                    startActivity(Intent(this@MainActivity, ChannelsActivity::class.java))
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error al cargar la lista: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun loadFromUri(uri: Uri) {
        setLoading(true)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val content = PlaylistFetcher.fetchFromUri(this@MainActivity, uri)
                val channels = withContext(Dispatchers.Default) { M3uParser.parse(content) }
                if (channels.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "No se encontraron canales en el archivo",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    PlaylistRepository.channels = channels
                    PlaylistRepository.currentPlaylistName = "Archivo local"
                    startActivity(Intent(this@MainActivity, ChannelsActivity::class.java))
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error al leer el archivo: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLoadUrl.isEnabled = !loading
        binding.btnLoadFile.isEnabled = !loading
    }
}
