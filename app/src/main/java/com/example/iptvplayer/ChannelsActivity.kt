package com.example.iptvplayer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.iptvplayer.adapter.ChannelAdapter
import com.example.iptvplayer.data.FavoritesManager
import com.example.iptvplayer.data.PlaylistRepository
import com.example.iptvplayer.databinding.ActivityChannelsBinding
import com.example.iptvplayer.model.Channel

class ChannelsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChannelsBinding
    private lateinit var favoritesManager: FavoritesManager
    private lateinit var adapter: ChannelAdapter

    private var allChannels: List<Channel> = emptyList()
    private var categories: List<String> = listOf("Todos")
    private var currentCategory: String = "Todos"
    private var currentQuery: String = ""
    private var showOnlyFavorites: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChannelsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = PlaylistRepository.currentPlaylistName.ifBlank { "Canales" }

        favoritesManager = FavoritesManager(this)
        allChannels = PlaylistRepository.channels

        adapter = ChannelAdapter(
            channels = emptyList(),
            isFavorite = { favoritesManager.isFavorite(it.streamUrl) },
            onClick = { channel -> openPlayer(channel) },
            onFavoriteClick = { channel ->
                favoritesManager.toggleFavorite(channel.streamUrl)
                applyFilters()
            }
        )
        binding.recyclerChannels.layoutManager = LinearLayoutManager(this)
        binding.recyclerChannels.adapter = adapter

        setupCategorySpinner()

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText ?: ""
                applyFilters()
                return true
            }
        })

        binding.chipFavorites.setOnClickListener {
            showOnlyFavorites = binding.chipFavorites.isChecked
            applyFilters()
        }

        applyFilters()
    }

    override fun onResume() {
        super.onResume()
        // Refresca por si se cambiaron favoritos desde el reproductor.
        applyFilters()
    }

    private fun setupCategorySpinner() {
        val categoryList = mutableListOf("Todos")
        categoryList.addAll(allChannels.mapNotNull { it.groupTitle }.distinct().sorted())
        categories = categoryList

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = spinnerAdapter
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentCategory = categories.getOrElse(position) { "Todos" }
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun applyFilters() {
        var filtered = allChannels

        if (showOnlyFavorites) {
            val favs = favoritesManager.getFavorites()
            filtered = filtered.filter { favs.contains(it.streamUrl) }
        }

        if (currentCategory != "Todos") {
            filtered = filtered.filter { it.groupTitle == currentCategory }
        }

        if (currentQuery.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(currentQuery, ignoreCase = true) }
        }

        adapter.updateList(filtered)
        binding.txtEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openPlayer(channel: Channel) {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra(PlayerActivity.EXTRA_CHANNEL, channel)
        startActivity(intent)
    }
}
