package com.example.customerlauncher.ui.favorite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.customerlauncher.R
import com.example.customerlauncher.domain.model.FavoriteContent

class FavoriteContentAdapter(
    private val contents: List<FavoriteContent>,
    private val onClick: (FavoriteContent) -> Unit
) : RecyclerView.Adapter<FavoriteContentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumb: ImageView = view.findViewById(R.id.contentThumbnail)
        val title: TextView = view.findViewById(R.id.contentTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite_content, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val content = contents[position]
        holder.title.text = content.title
        Glide.with(holder.itemView.context)
            .load(content.thumbnailUrl)
            .into(holder.thumb)
        holder.itemView.setOnClickListener { onClick(content) }
    }

    override fun getItemCount() = contents.size
}
