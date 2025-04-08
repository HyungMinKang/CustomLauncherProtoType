package com.example.customerlauncher.ui.main

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.VideoView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.customerlauncher.R
import com.example.customerlauncher.VideoPlayerActivity
import kotlinx.coroutines.*

class ContentAdapter(private val contentList: List<ContentData>) :
    RecyclerView.Adapter<ContentAdapter.ContentViewHolder>() {

    private val previewCache = mutableMapOf<String, Bitmap>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_content, parent, false)
        return ContentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
        val contentData = contentList[position]
        holder.contentNameTextView.text = contentData.name

        // 캐시된 이미지가 있으면 바로 설정, 없으면 비동기 로딩
        previewCache[contentData.videoUri]?.let {
            holder.contentView.setBackgroundDrawable(android.graphics.drawable.BitmapDrawable(holder.contentView.resources, it))
            holder.contentView.visibility = View.VISIBLE
        } ?: run {
            holder.contentView.setBackgroundResource(R.drawable.placeholder)
            holder.contentView.visibility = View.VISIBLE
            loadVideoPreview(holder.contentView, contentData.videoUri)
        }

        holder.contentView.setMediaController(null)

        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            v.animate()
                .scaleX(if (hasFocus) 1.1f else 1.0f)
                .scaleY(if (hasFocus) 1.1f else 1.0f)
                .setDuration(200)
                .start()
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, VideoPlayerActivity::class.java)
            intent.putExtra("videoUri", contentData.videoUri)
            startActivity(holder.itemView.context, intent, null)
        }
    }

    private fun loadVideoPreview(videoView: VideoView, videoUri: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(videoView.context, Uri.parse(videoUri))
                    val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

                    bitmap?.let {
                        val targetWidth = 150 // 적절한 크기로 조절
                        val targetHeight = 100 // 적절한 크기로 조절
                        val scaledBitmap = Bitmap.createScaledBitmap(it, targetWidth, targetHeight, false)
                        withContext(Dispatchers.Main) {
                            previewCache[videoUri] = scaledBitmap
                            videoView.setBackgroundDrawable(android.graphics.drawable.BitmapDrawable(videoView.resources, scaledBitmap))
                            videoView.visibility = View.VISIBLE
                        }
                    }
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    videoView.setBackgroundResource(R.drawable.placeholder)
                    videoView.visibility = View.VISIBLE
                    e.printStackTrace()
                }
            }
        }
    }

    override fun getItemCount(): Int = contentList.size

    override fun onViewRecycled(holder: ContentViewHolder) {
        super.onViewRecycled(holder)
        holder.contentView.background = null // 배경 이미지 해제
        coroutineScope.coroutineContext.cancelChildren() // 진행 중인 코루틴 작업 취소
    }

    class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contentView: VideoView = itemView.findViewById(R.id.contentView)
        val contentNameTextView: TextView = itemView.findViewById(R.id.contentNameTextView)
    }

    data class ContentData(val videoUri: String, val name: String)
}