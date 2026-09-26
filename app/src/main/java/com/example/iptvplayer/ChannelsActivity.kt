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
import com.example.iptvplayer.data.RecentChannelsManager
import com.example.iptvplayer.databinding.ActivityChannelsBinding
import com.example.iptvplayer.model.Channel
import com.example.iptvplayer.model.ContentModule

class ChannelsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChannelsBinding
    private lateinit var favoritesManager: FavoritesManager
    private lateinit var recentManager: RecentChannelsManager
    private lateinit var adapter: ChannelAdapter

    private var allChannels: List<Channel> = emptyList()
    private var categories: List<String> = listOf("Todos")
    private var currentCategory: String = "Todos"
    private var currentQuery: String = ""
    private var showOnlyFavorites: Boolean = false
    private var showOnlyRecent: Boolean = false
    private lateinit var module: ContentModule

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChannelsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        module = ContentModule.fromId(intent.getStringExtra(EXTRA_MODULE_ID))

        favoritesManager = FavoritesManager(this)
        recentManager = RecentChannelsManager(this)

        val sourceChannels = PlaylistRepository.channels
        allChannels = sourceChannels.filter { ContentModule.inferFrom(it) == module }

        binding.txtModuleTitle.text = module.displayName
        title = "${module.displayName} · ${PlaylistRepository.currentPlaylistName.ifBlank { getString(R.string.app_name) }}"

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

        binding.chipRecent.setOnClickListener {
            showOnlyRecent = binding.chipRecent.isChecked
            applyFilters()
        }

        applyFilters()
    }

    override fun onResume() {
        super.onResume()
        applyFilters()
    }

    private fun setupCategorySpinner() {
        val categoryList = mutableListOf(getString(R.string.all_categories))
        categoryList.addAll(allChannels.mapNotNull { it.groupTitle }.distinct().sorted())
        categories = categoryList

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = spinnerAdapter
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentCategory = categories.getOrElse(position) { getString(R.string.all_categories) }
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun applyFilters() {
        var filtered = allChannels

        if (showOnlyFavorites) {
            val favs = favoritesManager.getFavorites()
            filtered = filtered.filter { favs.contains(it.streamUrl) }
        }

        if (showOnlyRecent) {
            val recents = recentManager.getRecent()
            val recentSet = recents.toSet()
            filtered = filtered.filter { recentSet.contains(it.streamUrl) }
                .sortedBy { recents.indexOf(it.streamUrl).let { index -> if (index == -1) Int.MAX_VALUE else index } }
        }

        if (currentCategory != getString(R.string.all_categories)) {
            filtered = filtered.filter { it.groupTitle == currentCategory }
        }

        if (currentQuery.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(currentQuery, ignoreCase = true) }
        }

        adapter.updateList(filtered)
        binding.txtEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openPlayer(channel: Channel) {
        recentManager.push(channel.streamUrl)
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra(PlayerActivity.EXTRA_CHANNEL, channel)
        intent.putExtra(PlayerActivity.EXTRA_MODULE_ID, module.id)
        startActivity(intent)
    }

    companion object {
        const val EXTRA_MODULE_ID = "extra_module_id"
    }
}
