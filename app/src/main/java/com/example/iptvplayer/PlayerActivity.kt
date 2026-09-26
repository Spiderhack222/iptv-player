package com.example.iptvplayer

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.example.iptvplayer.data.PlaylistRepository
import com.example.iptvplayer.databinding.ActivityPlayerBinding
import com.example.iptvplayer.model.Channel
import com.example.iptvplayer.model.ContentModule

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private lateinit var channel: Channel
    private lateinit var module: ContentModule

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("DEPRECATION")
        val extraChannel = intent.getSerializableExtra(EXTRA_CHANNEL) as? Channel
        if (extraChannel == null) {
            finish()
            return
        }

        module = ContentModule.fromId(intent.getStringExtra(EXTRA_MODULE_ID))
        channel = extraChannel

        binding.btnNext.setOnClickListener { switchChannel(1) }
        binding.btnPrev.setOnClickListener { switchChannel(-1) }
        binding.btnBack.setOnClickListener { finish() }

        bindChannelInfo()
        preparePlayer()
    }

    private fun currentModuleList(): List<Channel> {
        val list = PlaylistRepository.channels.filter { ContentModule.inferFrom(it) == module }
        return if (list.isEmpty()) PlaylistRepository.channels else list
    }

    private fun bindChannelInfo() {
        val list = currentModuleList()
        val currentIndex = list.indexOfFirst { it.streamUrl == channel.streamUrl }

        binding.txtChannelName.text = channel.name
        binding.txtChannelMeta.text = channel.groupTitle ?: getString(R.string.uncategorized)
        binding.txtChannelIndex.text = getString(
            R.string.channel_position,
            (if (currentIndex == -1) 1 else currentIndex + 1),
            list.size
        )

        Glide.with(this)
            .load(channel.logoUrl)
            .placeholder(R.drawable.ic_tv_placeholder)
            .error(R.drawable.ic_tv_placeholder)
            .into(binding.imgChannelLogo)
    }

    private fun switchChannel(direction: Int) {
        val list = currentModuleList()
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.streamUrl == channel.streamUrl }
        if (currentIndex == -1) return
        val nextIndex = (currentIndex + direction + list.size) % list.size
        channel = list[nextIndex]
        bindChannelInfo()
        preparePlayer()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun preparePlayer() {
        player?.release()
        binding.progressBar.visibility = View.VISIBLE
        binding.txtError.visibility = View.GONE

        val exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer
        player = exoPlayer

        val mediaItem = MediaItem.fromUri(channel.streamUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                binding.progressBar.visibility = View.GONE
                binding.txtError.visibility = View.VISIBLE
                binding.txtError.text = getString(R.string.player_error, error.errorCodeName)
            }
        })
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                switchChannel(1)
                true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                switchChannel(-1)
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }

    companion object {
        const val EXTRA_CHANNEL = "extra_channel"
        const val EXTRA_MODULE_ID = "extra_module_id"
    }
}
