package com.example.customerlauncher.ui.favorite

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.customerlauncher.R
import com.example.customerlauncher.VideoPlayerActivity
import com.example.customerlauncher.domain.model.FavoriteContent
import com.example.customerlauncher.domain.model.WeatherTheme
import com.example.customerlauncher.domain.model.WeatherThemeWithLed
import com.example.customerlauncher.ui.main.MainActivity
import com.example.customerlauncher.ui.main.ThemeFragment
import com.example.customerlauncher.ui.ott.OttAdapter
import org.json.JSONArray


class FavoriteFragment : Fragment(), ThemeFragment {

    private lateinit var adapter: FavoriteItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.favoriteUnifiedRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val pm = requireActivity().packageManager
        val theme = (activity as? MainActivity)?.getCurrentTheme()
        val textColor = if (theme?.isDarkText == true) Color.BLACK else Color.WHITE
        view.findViewById<TextView>(R.id.favoriteSectionTitle).setTextColor(textColor)
        adapter = FavoriteItemAdapter(
            pm, textColor,
            onAppClick = {
                val intent = pm.getLaunchIntentForPackage(it.activityInfo.packageName)
                if (intent != null) startActivity(intent)
                else Toast.makeText(requireContext(), "앱을 실행할 수 없습니다", Toast.LENGTH_SHORT).show()
            },
            onContentClick = {
                val intent = Intent(requireContext(), VideoPlayerActivity::class.java)
                intent.putExtra("videoUri", it.videoUrl)
                startActivity(intent)
            }
        )

        recyclerView.adapter = adapter
        reloadFavorites()
    }

    override fun onResume() {
        super.onResume()
        reloadFavorites() // Fragment 재진입 시 갱신
    }

    private fun reloadFavorites() {
        val pm = requireActivity().packageManager
        val apps = loadFavoriteAppPackages().let { packages ->
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .filter { it.activityInfo.packageName in packages }
                .map { FavoriteItem.App(it) }
        }
        val contents = loadFavoriteContents().map { FavoriteItem.Content(it) }
        adapter.updateData(apps + contents)
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

    override fun onThemeChanged(theme: WeatherTheme) {
        view?.let { MainActivity().applyTextColorToAll(it, if (theme.isDarkText) Color.BLACK else Color.WHITE) }
    }


}
