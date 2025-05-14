package com.example.customerlauncher.ui.favorite

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.customerlauncher.R
import com.example.customerlauncher.domain.model.FavoriteContent
import com.example.customerlauncher.ui.ott.OttAdapter
import org.json.JSONArray


class FavoriteFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_favorite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val pm = requireActivity().packageManager

        // 1. 앱 즐겨찾기 처리
        val appRecyclerView = view.findViewById<RecyclerView>(R.id.favoriteAppRecyclerView)
        appRecyclerView.layoutManager = GridLayoutManager(context, 4)
        val appPackages = loadFavoriteAppPackages()
        val allApps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }, 0)
        val favoriteApps = allApps.filter { it.activityInfo.packageName in appPackages }
        appRecyclerView.adapter = OttAdapter(
            favoriteApps,
            pm,
            onClick = { app ->
                val intent = pm.getLaunchIntentForPackage(app.activityInfo.packageName)
                intent?.let { startActivity(it) }
            },
            onLongClick = { app ->
                val pkg = app.activityInfo.packageName
                val context = requireContext()
                val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
                prefs.edit().remove(pkg).apply()
                Toast.makeText(context, "☆ '${pkg}' 즐겨찾기에서 제거됨", Toast.LENGTH_SHORT).show()
                // 어댑터 갱신 필요
                onViewCreated(requireView(), null) // 간단한 방식으로 리로딩
                true
            }
        )

        // 2. 콘텐츠 즐겨찾기 처리
        val contentRecyclerView = view.findViewById<RecyclerView>(R.id.favoriteContentRecyclerView)
        contentRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val contents = loadFavoriteContents()
        contentRecyclerView.adapter = FavoriteContentAdapter(contents) { content ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(content.videoUrl), "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    private fun loadFavoriteAppPackages(): List<String> {
        val pref = requireContext().getSharedPreferences("favorites", Context.MODE_PRIVATE)
        return pref.all.filterValues { it == true }.mapNotNull { it.key }
    }

    private fun loadFavoriteContents(): List<FavoriteContent> {
        val pref = requireContext().getSharedPreferences("favorites_content", Context.MODE_PRIVATE)
        val json = pref.getString("favorite_content_list", "[]") ?: "[]"
        val array = JSONArray(json)

        return List(array.length()) {
            val obj = array.getJSONObject(it)
            FavoriteContent(
                obj.getString("title"),
                obj.getString("thumbnailUrl"),
                obj.getString("videoUrl")
            )
        }
    }
}
