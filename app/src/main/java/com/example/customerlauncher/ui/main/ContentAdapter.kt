package com.example.customerlauncher.ui.main

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.customerlauncher.ContentFragment
import com.example.customerlauncher.R
import com.example.customerlauncher.VideoPlayerActivity
import org.json.JSONArray
import org.json.JSONObject

class ContentAdapter(private val fragment: ContentFragment, private val contentList: List<ContentData>, var textColor: Int) :
    RecyclerView.Adapter<ContentAdapter.ContentViewHolder>() {

    private val prefs = fragment.requireContext().getSharedPreferences("favorites", android.content.Context.MODE_PRIVATE)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_content, parent, false)
        return ContentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
        val contentData = contentList[position]
        holder.titleTextView.text = contentData.title
        holder.thumbnailImageView.setImageDrawable(null)
        holder.titleTextView.setTextColor(textColor)
        Glide.with(fragment)
            .load(contentData.thumbnailUrl)  // 또는 thumbnailUrl
            .placeholder(R.drawable.placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL) // 🔁 캐시 비활성화
            .skipMemoryCache(false)                     // 🔁 메모리 캐시도 비활성화
            .into(holder.thumbnailImageView)

        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            v.animate()
                .scaleX(if (hasFocus) 1.1f else 1.0f)
                .scaleY(if (hasFocus) 1.1f else 1.0f)
                .setDuration(200)
                .start()
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, VideoPlayerActivity::class.java)
            intent.putExtra("videoUri", contentData.videoUrl)
            startActivity(holder.itemView.context, intent, null)
        }
        holder.itemView.setOnLongClickListener {
            val context = holder.itemView.context
            val prefs = context.getSharedPreferences("favorites_content", Context.MODE_PRIVATE)
            val key = "favorite_content_list"
            val jsonStr = prefs.getString(key, "[]") ?: "[]"
            val array = JSONArray(jsonStr)

            // 이미 존재하는지 확인
            var foundIndex = -1
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("title") == contentData.title) {
                    foundIndex = i
                    break
                }
            }

            val message: String

            if (foundIndex >= 0) {
                // 즐겨찾기 해제
                val newArray = JSONArray()
                for (i in 0 until array.length()) {
                    if (i != foundIndex) newArray.put(array.getJSONObject(i))
                }
                prefs.edit().putString(key, newArray.toString()).apply()
                message = "☆ '${contentData.title}' 즐겨찾기에서 제거됨"
            } else {
                // 즐겨찾기 추가
                val obj = JSONObject().apply {
                    put("title", contentData.title)
                    put("thumbnailUrl", contentData.thumbnailUrl)
                    put("videoUrl", contentData.videoUrl)
                }
                array.put(obj)
                prefs.edit().putString(key, array.toString()).apply()
                message = "★ '${contentData.title}' 즐겨찾기에 추가됨"
            }

            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun getItemCount(): Int = contentList.size

    override fun getItemViewType(position: Int): Int {
        return position // ➜ 각 아이템마다 새로운 뷰타입 부여 → ViewHolder 재사용 방지
    }

    class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnailImageView: ImageView = itemView.findViewById(R.id.thumbnailImageView)
        val titleTextView: TextView = itemView.findViewById(R.id.contentTitleTextView)
    }

    data class ContentData(
        val title: String,
        val thumbnailUrl: String,
        val videoUrl: String
    )
}