package com.example.customerlauncher.ui.main

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.customerlauncher.ContentFragment
import com.example.customerlauncher.R
import com.example.customerlauncher.VideoPlayerActivity

class ContentAdapter(private val fragment: ContentFragment, private val contentList: List<ContentData>) :
    RecyclerView.Adapter<ContentAdapter.ContentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_content, parent, false)
        return ContentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
        val contentData = contentList[position]
        holder.titleTextView.text = contentData.title
        holder.thumbnailImageView.setImageDrawable(null)

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