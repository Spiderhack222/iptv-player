package com.example.iptvplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.iptvplayer.R
import com.example.iptvplayer.model.Channel

class ChannelAdapter(
    private var channels: List<Channel>,
    private val isFavorite: (Channel) -> Boolean,
    private val onClick: (Channel) -> Unit,
    private val onFavoriteClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    inner class ChannelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val logo: ImageView = view.findViewById(R.id.imgLogo)
        val name: TextView = view.findViewById(R.id.txtName)
        val group: TextView = view.findViewById(R.id.txtGroup)
        val favButton: ImageButton = view.findViewById(R.id.btnFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]
        holder.name.text = channel.name
        holder.group.text = channel.groupTitle ?: "Sin categoría"

        Glide.with(holder.logo.context)
            .load(channel.logoUrl)
            .placeholder(R.drawable.ic_tv_placeholder)
            .error(R.drawable.ic_tv_placeholder)
            .into(holder.logo)

        val favorite = isFavorite(channel)
        holder.favButton.setImageResource(
            if (favorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        )

        holder.itemView.setOnClickListener { onClick(channel) }
        holder.favButton.setOnClickListener { onFavoriteClick(channel) }
    }

    override fun getItemCount(): Int = channels.size

    fun updateList(newChannels: List<Channel>) {
        channels = newChannels
        notifyDataSetChanged()
    }
}
