package com.example.customerlauncher

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.widget.HorizontalGridView
import com.example.customerlauncher.ui.main.ImageAdapter

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ContentFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ContentFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var imageGridView: HorizontalGridView
    private var imageAdapter: ImageAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_content, container, false)
        imageGridView = view.findViewById(R.id.imageGridView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 이미지 데이터 생성 (실제 데이터로 대체)
        val imageList: List<ImageAdapter.ImageData> = generateImageList()
        imageAdapter = ImageAdapter(imageList)
        imageGridView.adapter = imageAdapter

        // 초기 포커스 설정
        imageGridView.requestFocus()
    }

    private fun generateImageList(): List<ImageAdapter.ImageData> {
        val list = mutableListOf<ImageAdapter.ImageData>()
        for (i in 0 until 20) {
            list.add(ImageAdapter.ImageData("Image ${i + 1}"))
        }
        return list
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ContentFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ContentFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}