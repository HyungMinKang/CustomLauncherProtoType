package com.example.customerlauncher.ui.favorite

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.customerlauncher.R
import com.example.customerlauncher.domain.model.FavoriteContent
import org.json.JSONArray

class FavoriteContentAdapter(
    private var contents: List<FavoriteContent>,
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
        holder.itemView.setOnLongClickListener {
            val context = holder.itemView.context
            val prefs = context.getSharedPreferences("favorites_content", Context.MODE_PRIVATE)
            val key = "favorite_content_list"
            val jsonStr = prefs.getString(key, "[]") ?: "[]"
            val array = JSONArray(jsonStr)

            val newArray = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("title") != content.title) {
                    newArray.put(obj)
                }
            }

            prefs.edit().putString(key, newArray.toString()).apply()
            Toast.makeText(context, "☆ '${content.title}' 즐겨찾기에서 제거됨", Toast.LENGTH_SHORT).show()

            // UI 갱신
            contents = contents.filter { it.title != content.title }
            notifyDataSetChanged()
            true
        }
    }

    override fun getItemCount() = contents.size
}
