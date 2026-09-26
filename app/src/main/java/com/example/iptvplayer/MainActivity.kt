package com.example.iptvplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.iptvplayer.adapter.PlaylistAdapter
import com.example.iptvplayer.data.DeviceProfileStorage
import com.example.iptvplayer.data.PlaylistRepository
import com.example.iptvplayer.data.PlaylistStorage
import com.example.iptvplayer.databinding.ActivityMainBinding
import com.example.iptvplayer.model.ContentModule
import com.example.iptvplayer.model.DeviceProfile
import com.example.iptvplayer.model.Playlist
import com.example.iptvplayer.util.M3uParser
import com.example.iptvplayer.util.PlaylistFetcher
import com.example.iptvplayer.util.RemotePlaylistResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var playlistStorage: PlaylistStorage
    private lateinit var deviceProfileStorage: DeviceProfileStorage
    private lateinit var adapter: PlaylistAdapter

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { loadFromUri(it) }
        }

    private val macActivationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val profile = deviceProfileStorage.getProfile()
                if (profile != null) {
                    loadFromMacProfile(profile, openLiveOnSuccess = true)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playlistStorage = PlaylistStorage(this)
        deviceProfileStorage = DeviceProfileStorage(this)

        adapter = PlaylistAdapter(
            playlistStorage.getPlaylists(),
            onClick = { playlist -> loadFromUrl(playlist.source, playlist.name, save = false, openLiveOnSuccess = true) },
            onDelete = { playlist ->
                playlistStorage.removePlaylist(playlist)
                adapter.updateList(playlistStorage.getPlaylists())
            }
        )

        binding.recyclerPlaylists.layoutManager = LinearLayoutManager(this)
        binding.recyclerPlaylists.adapter = adapter

        setupActions()
        setupModuleCards()
        setupLoadingInputs()

        updateUiStatus()

        val profile = deviceProfileStorage.getProfile()
        if (PlaylistRepository.channels.isEmpty() && profile?.autoReconnect == true) {
            loadFromMacProfile(profile, openLiveOnSuccess = false)
        }
    }

    private fun setupLoadingInputs() {
        binding.btnLoadUrl.setOnClickListener {
            val url = binding.editUrl.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, R.string.url_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            askNameAndLoad(url)
        }

        binding.btnLoadFile.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }
    }

    private fun setupModuleCards() {
        val mapping = mapOf(
            binding.cardLiveTv to ContentModule.LIVE_TV,
            binding.cardMovies to ContentModule.MOVIES,
            binding.cardSeries to ContentModule.SERIES,
            binding.cardRadios to ContentModule.RADIOS
        )

        mapping.forEach { (view, module) ->
            view.setOnClickListener { openModule(module) }
            view.setOnFocusChangeListener { focusedView, hasFocus ->
                focusedView.scaleX = if (hasFocus) 1.06f else 1f
                focusedView.scaleY = if (hasFocus) 1.06f else 1f
                focusedView.alpha = if (hasFocus) 1f else 0.95f
            }
            view.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_UP &&
                    (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                ) {
                    openModule(module)
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun setupActions() {
        binding.btnRefreshHome.setOnClickListener {
            when {
                PlaylistRepository.currentPlaylistSource.isNotBlank() -> {
                    loadFromUrl(
                        PlaylistRepository.currentPlaylistSource,
                        PlaylistRepository.currentPlaylistName.ifBlank { getString(R.string.default_playlist_name) },
                        save = false,
                        openLiveOnSuccess = false
                    )
                }

                deviceProfileStorage.getProfile() != null -> {
                    loadFromMacProfile(deviceProfileStorage.getProfile()!!, openLiveOnSuccess = false)
                }

                else -> Toast.makeText(this, R.string.no_playlist_to_refresh, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnChangePlaylist.setOnClickListener {
            binding.editUrl.requestFocus()
            binding.editUrl.setSelection(binding.editUrl.text?.length ?: 0)
            Toast.makeText(this, R.string.change_playlist_hint, Toast.LENGTH_SHORT).show()
        }

        binding.btnLanguage.setOnClickListener {
            val current = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "es"
            val next = if (current == "es") "en" else "es"
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(next))
            updateLanguageButtonLabel()
        }

        binding.btnActivateMac.setOnClickListener {
            val intent = Intent(this, MacActivationActivity::class.java)
            intent.putExtra(MacActivationActivity.EXTRA_DEFAULT_MAC, buildDeviceMacAddress())
            macActivationLauncher.launch(intent)
        }

        binding.btnDeviceInfo.setOnClickListener { showDeviceInfo() }
        binding.btnPower.setOnClickListener { finishAffinity() }

        updateLanguageButtonLabel()
    }

    private fun updateLanguageButtonLabel() {
        val current = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "es"
        val label = if (current == "es") getString(R.string.language_spanish) else getString(R.string.language_english)
        binding.btnLanguage.text = getString(R.string.language_button, label)
    }

    private fun showDeviceInfo() {
        val profile = deviceProfileStorage.getProfile()
        val mac = profile?.macAddress ?: buildDeviceMacAddress()
        val portal = profile?.portalUrl ?: getString(R.string.not_configured)
        AlertDialog.Builder(this)
            .setTitle(R.string.device_info)
            .setMessage(
                getString(
                    R.string.device_info_body,
                    mac,
                    portal
                )
            )
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun openModule(module: ContentModule) {
        if (PlaylistRepository.channels.isEmpty()) {
            Toast.makeText(this, R.string.load_playlist_first, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, ChannelsActivity::class.java)
        intent.putExtra(ChannelsActivity.EXTRA_MODULE_ID, module.id)
        startActivity(intent)
    }

    private fun askNameAndLoad(source: String) {
        val input = EditText(this)
        input.hint = getString(R.string.playlist_name_hint)
        AlertDialog.Builder(this)
            .setTitle(R.string.save_playlist_title)
            .setView(input)
            .setPositiveButton(R.string.load_action) { _, _ ->
                val name = input.text.toString().ifBlank { getString(R.string.default_playlist_name) }
                loadFromUrl(source, name, save = true, openLiveOnSuccess = true)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun loadFromMacProfile(profile: DeviceProfile, openLiveOnSuccess: Boolean) {
        val url = RemotePlaylistResolver.buildUrl(profile)
        loadFromUrl(url, profile.profileName.ifBlank { getString(R.string.mac_default_profile_name) }, save = false, openLiveOnSuccess = openLiveOnSuccess)
    }

    private fun loadFromUrl(url: String, name: String, save: Boolean, openLiveOnSuccess: Boolean) {
        setLoading(true)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val content = PlaylistFetcher.fetchFromUrl(url)
                val channels = withContext(Dispatchers.Default) { M3uParser.parse(content) }
                if (channels.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        R.string.no_channels_found,
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    if (save) {
                        playlistStorage.addPlaylist(Playlist(name = name, source = url))
                        adapter.updateList(playlistStorage.getPlaylists())
                    }
                    PlaylistRepository.channels = channels
                    PlaylistRepository.currentPlaylistName = name
                    PlaylistRepository.currentPlaylistSource = url

                    val profile = deviceProfileStorage.getProfile()
                    if (profile != null) {
                        deviceProfileStorage.saveProfile(profile.copy(lastConnectedAt = System.currentTimeMillis()))
                    }

                    updateUiStatus()
                    if (openLiveOnSuccess) {
                        openModule(ContentModule.LIVE_TV)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.playlist_load_error, e.message),
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
                        R.string.no_channels_in_file,
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    PlaylistRepository.channels = channels
                    PlaylistRepository.currentPlaylistName = getString(R.string.local_file_playlist)
                    PlaylistRepository.currentPlaylistSource = uri.toString()
                    updateUiStatus()
                    openModule(ContentModule.LIVE_TV)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.file_load_error, e.message),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun updateUiStatus() {
        binding.txtCurrentPlaylist.text = PlaylistRepository.currentPlaylistName.ifBlank {
            getString(R.string.not_configured)
        }

        val profile = deviceProfileStorage.getProfile()
        binding.txtMacValue.text = profile?.macAddress ?: buildDeviceMacAddress()

        binding.txtAccountStatus.text = if (profile?.lastConnectedAt != null && profile.lastConnectedAt > 0L) {
            val formattedDate = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(profile.lastConnectedAt))
            getString(R.string.account_active_since, formattedDate)
        } else {
            getString(R.string.account_not_connected)
        }
    }

    private fun buildDeviceMacAddress(): String {
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            ?.uppercase()
            ?: "A1B2C3D4E5F6"
        val padded = (androidId + "ABCDEF123456").take(12)
        return padded.chunked(2).joinToString(":")
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLoadUrl.isEnabled = !loading
        binding.btnLoadFile.isEnabled = !loading
        binding.btnRefreshHome.isEnabled = !loading
    }
}
