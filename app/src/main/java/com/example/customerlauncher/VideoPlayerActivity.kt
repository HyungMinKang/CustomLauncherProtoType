package com.example.customerlauncher

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.fragment.app.FragmentActivity

class VideoPlayerActivity : FragmentActivity() {

    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        videoView = findViewById(R.id.videoViewFull)

        val videoUriString = intent.getStringExtra("videoUri")
        videoUriString?.let {
            val videoUri = Uri.parse(it)
            videoView.setVideoURI(videoUri)
            videoView.setMediaController(android.widget.MediaController(this))
            videoView.requestFocus()
            videoView.start()
        }
    }
}