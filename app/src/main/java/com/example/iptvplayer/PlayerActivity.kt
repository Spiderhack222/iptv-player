package com.example.iptvplayer

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
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

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private lateinit var channel: Channel

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
        channel = extraChannel

        bindChannelInfo()

        binding.btnNext.setOnClickListener { switchChannel(1) }
        binding.btnPrev.setOnClickListener { switchChannel(-1) }
        binding.btnBack.setOnClickListener { finish() }

        preparePlayer()
    }

    private fun bindChannelInfo() {
        binding.txtChannelName.text = channel.name
        Glide.with(this)
            .load(channel.logoUrl)
            .placeholder(R.drawable.ic_tv_placeholder)
            .error(R.drawable.ic_tv_placeholder)
            .into(binding.imgChannelLogo)
    }

    private fun switchChannel(direction: Int) {
        val list = PlaylistRepository.channels
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
                binding.txtError.text = "No se pudo reproducir el canal (${error.errorCodeName})"
            }
        })
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
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
    }
}
