package com.example.customerlauncher.ui.main

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.customerlauncher.R
import com.example.customerlauncher.VideoPlayerActivity

class ContentAdapter(private val contentList: List<ContentData>) :
    RecyclerView.Adapter<ContentAdapter.ContentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_content, parent, false)
        return ContentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
        val contentData = contentList[position]
        holder.contentNameTextView.text = contentData.name

        // 미리보기 이미지 설정 (VideoView의 첫 프레임 사용)
        setVideoPreview(holder.contentView, contentData.videoUri)
        holder.contentView.setMediaController(null) // MediaController 제거

        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            v.animate()
                .scaleX(if (hasFocus) 1.1f else 1.0f)
                .scaleY(if (hasFocus) 1.1f else 1.0f)
                .setDuration(200)
                .start()
            if (hasFocus) {
                // 포커스 받으면 시각적인 변화만 (확대)
            } else {
                // 포커스 잃으면 원래 크기로
            }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, VideoPlayerActivity::class.java)
            intent.putExtra("videoUri", contentData.videoUri)
            startActivity(holder.itemView.context, intent, null)
        }
    }

    private fun setVideoPreview(videoView: VideoView, videoUri: String) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoView.context, Uri.parse(videoUri))
            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            videoView.setBackgroundDrawable(android.graphics.drawable.BitmapDrawable(videoView.resources, bitmap))
            videoView.visibility = View.VISIBLE // VideoView를 보이게 설정
        } catch (e: Exception) {
            // 미리보기 생성 실패 시 처리 (예: 기본 이미지 설정)
            videoView.setBackgroundResource(R.drawable.placeholder)
            videoView.visibility = View.VISIBLE
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = contentList.size

    class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contentView: VideoView = itemView.findViewById(R.id.contentView)
        val contentNameTextView: TextView = itemView.findViewById(R.id.contentNameTextView)
    }

    data class ContentData(val videoUri: String, val name: String)
}