package com.example.customerlauncher

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.widget.HorizontalGridView
import com.example.customerlauncher.ui.main.ContentAdapter
import com.example.customerlauncher.ui.main.ContentAdapter.ContentData
import com.example.customerlauncher.ui.main.MainActivity

class ContentFragment : Fragment() {

    private lateinit var videoGridView: HorizontalGridView
    private var contentAdapter: ContentAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_content, container, false)
        videoGridView = view.findViewById(R.id.contentGridView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val theme = (activity as? MainActivity)?.getCurrentTheme()
        val textColor = if (theme?.isDarkText == true) Color.BLACK else Color.WHITE
        val contentList = listOf(
            ContentData("Interstellar", "https://image.tmdb.org/t/p/w500/rAiYTfKGqDCRIIqo664sY9XZIvQ.jpg", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
            ContentData("The Dark Knight", "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"),
            ContentData("Endgame", "https://image.tmdb.org/t/p/w500/or06FN3Dka5tukK1e9sl16pB3iy.jpg", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/VolkswagenGTIReview.mp4"),
            ContentData("Tenet", "https://image.tmdb.org/t/p/w500/k68nPLbIST6NP96JmTxmZijEvCA.jpg", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4"),
            ContentData("The Matrix", "https://image.tmdb.org/t/p/w500/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4"),
            ContentData("Dune", "https://image.tmdb.org/t/p/w500/d5NXSklXo0qyIYkgV94XAgMIckC.jpg", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4")
        )

        contentAdapter = ContentAdapter(this,contentList, textColor)
        videoGridView.adapter = contentAdapter
        videoGridView.setHasFixedSize(true)
    }


}
