package com.example.customerlauncher

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.widget.HorizontalGridView
import com.example.customerlauncher.ui.main.ContentAdapter
import com.example.customerlauncher.ui.main.ContentAdapter.ContentData

class ContentFragment : Fragment() {

    private lateinit var videoGridView: HorizontalGridView
    private var contentAdapter: ContentAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_content, container, false)
        videoGridView = view.findViewById(R.id.contentGridView) as HorizontalGridView
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val contentList: List<ContentData> = generateContentList()
        contentAdapter = ContentAdapter(contentList)
        videoGridView.adapter = contentAdapter

        // HorizontalGridView의 크기가 고정되어 있다면 성능 향상을 위해 설정
        videoGridView.setHasFixedSize(true)

        //videoGridView.requestFocus()
    }

    private fun generateContentList(): List<ContentData> {
        val list = mutableListOf<ContentData>()
        list.add(ContentData("android.resource://${requireContext().packageName}/${R.raw.test_video}", "Test Video 1"))
        list.add(ContentData("android.resource://${requireContext().packageName}/${R.raw.test_video}", "Test Video 2"))
        list.add(ContentData("android.resource://${requireContext().packageName}/${R.raw.test_video}", "Test Video 3"))
        list.add(ContentData("android.resource://${requireContext().packageName}/${R.raw.test_video}", "Test Video 4"))
        list.add(ContentData("android.resource://${requireContext().packageName}/${R.raw.test_video}", "Test Video 5"))
        list.add(ContentData("android.resource://${requireContext().packageName}/${R.raw.test_video}", "Test Video 6"))
        list.add(ContentData("android.resource://${requireContext().packageName}/${R.raw.test_video}", "Test Video 7"))
        list.add(ContentData("android.resource://${requireContext().packageName}/${R.raw.test_video}", "Test Video 8"))
        list.add(ContentData("android.resource://${requireContext().packageName}/${R.raw.test_video}", "Test Video 9"))
        list.add(ContentData("android.resource://${requireContext().packageName}/${R.raw.test_video}", "Test Video 10"))

        return list
    }
}