package com.example.customerlauncher.ui.favorite

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.customerlauncher.R
import com.example.customerlauncher.domain.model.FavoriteContent
import org.json.JSONArray

class FavoriteItemAdapter(private val pm: PackageManager, var textColor: Int, private val onAppClick: (ResolveInfo) -> Unit, private val onContentClick: (FavoriteContent) -> Unit) : RecyclerView.Adapter<FavoriteItemAdapter.FavoriteViewHolder>() {

    private val items = mutableListOf<FavoriteItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite_content, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<FavoriteItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class FavoriteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image: ImageView = view.findViewById(R.id.imageView)
        private val title: TextView = view.findViewById(R.id.titleTextView)

        fun bind(item: FavoriteItem) {
            when (item) {
                is FavoriteItem.App -> {
                    val info = item.resolveInfo
                    image.setImageDrawable(info.loadIcon(pm))
                    title.text = info.loadLabel(pm)
                    title.setTextColor(textColor)
                    itemView.setOnClickListener { onAppClick(info) }
                    itemView.setOnLongClickListener {
                        val context = itemView.context
                        val pkg = info.activityInfo.packageName
                        val sharedPrefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
                        sharedPrefs.edit().remove(pkg).apply()
                        val pos = adapterPosition
                        if (pos != RecyclerView.NO_POSITION) {
                            items.removeAt(pos)
                            notifyItemRemoved(pos)
                        }

                        true
                    }
                }
                is FavoriteItem.Content -> {
                    val content = item.content
                    Glide.with(itemView.context)
                        .load(content.thumbnailUrl)
                        .placeholder(R.drawable.placeholder)
                        .into(image)
                    title.text = content.title
                    title.setTextColor(textColor)
                    itemView.setOnClickListener { onContentClick(content) }
                    itemView.setOnLongClickListener {
                        val context = itemView.context
                        val sharedPrefs = context.getSharedPreferences("favorites_content", Context.MODE_PRIVATE)
                        val json = sharedPrefs.getString("favorite_content_list", "[]") ?: "[]"
                        val array = JSONArray(json)
                        val newArray = JSONArray()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            if (obj.getString("videoUrl") != content.videoUrl) {
                                newArray.put(obj)
                            }
                        }

                        sharedPrefs.edit().putString("favorite_content_list", newArray.toString()).apply()

                        val pos = adapterPosition
                        if (pos != RecyclerView.NO_POSITION) {
                            items.removeAt(pos)
                            notifyItemRemoved(pos)
                        }

                        true
                    }
                }
            }
        }
    }
}

sealed class FavoriteItem {
    data class App(val resolveInfo: ResolveInfo) : FavoriteItem()
    data class Content(val content: FavoriteContent) : FavoriteItem()
}